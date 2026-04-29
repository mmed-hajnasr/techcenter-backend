#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCE_DIR="${SCRIPT_DIR}/resources"

# API configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"

# Database configuration (matches application.properties defaults)
DATABASE__HOST="${DATABASE__HOST:-localhost}"
DATABASE__PORT="${DATABASE__PORT:-5432}"
DATABASE__NAME="${DATABASE__NAME:-techcenter}"
DATABASE__USERNAME="${DATABASE__USERNAME:-postgres}"
DATABASE__PASSWORD="${DATABASE__PASSWORD:-postgres}"

# User definitions (override in your environment as needed)
ADMIN_EMAIL="${ADMIN_EMAIL:-admin.test@example.com}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin_test}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-StrongPass123!}"

MODERATOR_EMAIL="${MODERATOR_EMAIL:-moderator.test@example.com}"
MODERATOR_USERNAME="${MODERATOR_USERNAME:-moderator_test}"
MODERATOR_PASSWORD="${MODERATOR_PASSWORD:-StrongPass123!}"

USER_EMAIL="${USER_EMAIL:-user.test@example.com}"
USER_USERNAME="${USER_USERNAME:-user_test}"
USER_PASSWORD="${USER_PASSWORD:-StrongPass123!}"

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Error: required command '$1' is not installed." >&2
        exit 1
    fi
}

sql_escape() {
    printf "%s" "$1" | sed "s/'/''/g"
}

signup_or_login() {
    local email="$1"
    local username="$2"
    local password="$3"

    local signup_payload
    signup_payload=$(jq -nc \
        --arg email "$email" \
        --arg username "$username" \
        --arg password "$password" \
        '{email:$email, username:$username, password:$password}')

    local signup_response
    signup_response=$(curl -sS -X POST "$BASE_URL/signup" \
        -H "Content-Type: application/json" \
        -d "$signup_payload" || true)

    local signup_token
    signup_token=$(printf "%s" "$signup_response" | jq -r '.accessToken // empty')

    if [[ -n "$signup_token" ]]; then
        printf "%s" "$signup_token"
        return
    fi

    local login_payload
    login_payload=$(jq -nc \
        --arg identifier "$email" \
        --arg password "$password" \
        '{identifier:$identifier, password:$password}')

    local login_response
    login_response=$(curl -sS -X POST "$BASE_URL/login" \
        -H "Content-Type: application/json" \
        -d "$login_payload")

    local login_token
    login_token=$(printf "%s" "$login_response" | jq -r '.accessToken // empty')

    if [[ -z "$login_token" ]]; then
        echo "Error: failed to signup/login user '$email'." >&2
        echo "Signup response: $signup_response" >&2
        echo "Login response: $login_response" >&2
        exit 1
    fi

    printf "%s" "$login_token"
}

set_role() {
    local email="$1"
    local role="$2"
    local safe_email
    safe_email=$(sql_escape "$email")

    PGPASSWORD="$DATABASE__PASSWORD" psql \
        -h "$DATABASE__HOST" \
        -p "$DATABASE__PORT" \
        -U "$DATABASE__USERNAME" \
        -d "$DATABASE__NAME" \
        -v ON_ERROR_STOP=1 \
        -c "UPDATE users SET role = '$role' WHERE lower(email) = lower('$safe_email');" >/dev/null

    local current_role
    current_role=$(PGPASSWORD="$DATABASE__PASSWORD" psql \
        -h "$DATABASE__HOST" \
        -p "$DATABASE__PORT" \
        -U "$DATABASE__USERNAME" \
        -d "$DATABASE__NAME" \
        -tA \
        -c "SELECT role FROM users WHERE lower(email) = lower('$safe_email') LIMIT 1;" | tr -d '[:space:]')

    if [[ "$current_role" != "$role" ]]; then
        echo "Error: failed to set role '$role' for '$email' (current: '$current_role')." >&2
        exit 1
    fi
}

setup_user() {
    local label="$1"
    local email="$2"
    local username="$3"
    local password="$4"
    local role="$5"

    echo "Setting up $label ($role): $email"

    local token
    token=$(signup_or_login "$email" "$username" "$password")

    if [[ "$role" != "USER" ]]; then
        set_role "$email" "$role"

        token=$(curl -sS -X POST "$BASE_URL/login" \
            -H "Content-Type: application/json" \
            -d "$(jq -nc --arg identifier "$email" --arg password "$password" '{identifier:$identifier, password:$password}')" |
            jq -r '.accessToken // empty')

        if [[ -z "$token" ]]; then
            echo "Error: failed to login after setting role '$role' for '$email'." >&2
            exit 1
        fi
    fi

    echo "  -> done"
    printf -v "${label}_TOKEN" '%s' "$token"
}

json_array() {
    jq -nc '$ARGS.positional' --args "$@"
}

api_post_json() {
    local path="$1"
    local token="$2"
    local payload="$3"

    curl -sS -X POST "$BASE_URL$path" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "$payload"
}

create_domain() {
    local token="$1"
    local name="$2"
    local description="$3"

    api_post_json "/admin/domains" "$token" \
        "$(jq -nc --arg name "$name" --arg description "$description" '{name:$name, description:$description}')" |
        jq -r '.domainId'
}

create_actualite() {
    local token="$1"
    local titre="$2"
    local contenu="$3"
    local date_publication="$4"
    local est_en_avant="$5"
    local photo_path="$6"

    local response actualite_id
    response=$(api_post_json "/moderator/actualites" "$token" \
        "$(jq -nc \
            --arg titre "$titre" \
            --arg contenu "$contenu" \
            --arg datePublication "$date_publication" \
            --argjson estEnAvant "$est_en_avant" \
            '{titre:$titre, contenu:$contenu, datePublication:$datePublication, estEnAvant:$estEnAvant}')")
    actualite_id=$(printf "%s" "$response" | jq -r '.actualiteId')

    if [[ -z "$actualite_id" || "$actualite_id" == "null" ]]; then
        echo "Error: failed to create actualite '$titre'." >&2
        echo "$response" >&2
        exit 1
    fi

    curl -sS -X PUT "$BASE_URL/moderator/actualites/$actualite_id/photo" \
        -H "Authorization: Bearer $token" \
        -F "photo=@${photo_path}" >/dev/null

    printf "%s" "$actualite_id"
}

create_researcher() {
    local token="$1"
    local name="$2"
    local biographie="$3"
    local domain_ids_json="$4"
    local photo_path="$5"

    local response researcher_id
    response=$(api_post_json "/admin/researchers" "$token" \
        "$(jq -nc \
            --arg name "$name" \
            --arg biographie "$biographie" \
            --argjson domainIds "$domain_ids_json" \
            '{name:$name, biographie:$biographie, domainIds:$domainIds}')")
    researcher_id=$(printf "%s" "$response" | jq -r '.researcherId')

    if [[ -z "$researcher_id" || "$researcher_id" == "null" ]]; then
        echo "Error: failed to create researcher '$name'." >&2
        echo "$response" >&2
        exit 1
    fi

    curl -sS -X PUT "$BASE_URL/admin/researchers/$researcher_id/photo" \
        -H "Authorization: Bearer $token" \
        -F "photo=@${photo_path}" >/dev/null

    printf "%s" "$researcher_id"
}

create_publication() {
    local token="$1"
    local titre="$2"
    local resume="$3"
    local doi="$4"
    local date_publication="$5"
    local researcher_ids_json="$6"
    local pdf_path="$7"

    local response publication_id
    response=$(api_post_json "/admin/publications" "$token" \
        "$(jq -nc \
            --arg titre "$titre" \
            --arg resume "$resume" \
            --arg doi "$doi" \
            --arg datePublication "$date_publication" \
            --argjson researcherIds "$researcher_ids_json" \
            '{titre:$titre, resume:$resume, doi:$doi, datePublication:$datePublication, researcherIds:$researcherIds}')")
    publication_id=$(printf "%s" "$response" | jq -r '.publicationId')

    if [[ -z "$publication_id" || "$publication_id" == "null" ]]; then
        echo "Error: failed to create publication '$titre'." >&2
        echo "$response" >&2
        exit 1
    fi

    curl -sS -X PUT "$BASE_URL/admin/publications/$publication_id/pdf" \
        -H "Authorization: Bearer $token" \
        -F "pdf=@${pdf_path}" >/dev/null

    printf "%s" "$publication_id"
}

seed_domains() {
    local token="$1"
    local -a domain_ids=()

    local domain_specs=(
        "Artificial Intelligence|Research and development in AI systems"
        "Machine Learning|Data-driven modeling, optimization, and prediction"
        "Data Science|Analytics, statistics, and scientific computing"
        "Quantum Computing|Quantum algorithms and next-generation systems"
        "Cybersecurity|Security engineering and resilient digital infrastructure"
        "Software Engineering|Robust design, delivery, and maintenance of software"
        "Health Informatics|Computing applied to healthcare and medical data"
        "Sustainable Technologies|Green innovation and resource-efficient systems"
    )

    for spec in "${domain_specs[@]}"; do
        IFS='|' read -r name description <<<"$spec"
        domain_ids+=("$(create_domain "$token" "$name" "$description")")
    done

    printf "%s\n" "${domain_ids[@]}"
}

seed_actualites() {
    local token="$1"
    local -a actualite_ids=()

    local -a photo_files=(
        "$RESOURCE_DIR/actualite/AI.jpg"
        "$RESOURCE_DIR/actualite/crypto.jpg"
        "$RESOURCE_DIR/actualite/quantum.jpg"
        "$RESOURCE_DIR/actualite/quantum2.jpg"
        "$RESOURCE_DIR/actualite/AI and crypto.webp"
    )
    local -a titles=(
        "AI research momentum"
        "Cryptography and trust"
        "Quantum lab highlights"
        "Hybrid intelligence workshop"
        "AI and crypto convergence"
    )
    local -a contents=(
        "Recent progress in applied artificial intelligence across research teams."
        "Advances in cryptography and secure systems for modern applications."
        "Highlights from quantum computing experiments and prototypes."
        "A workshop exploring the intersection of AI, systems, and innovation."
        "A feature story on the convergence of AI methods and cryptographic tools."
    )
    local -a dates=(
        "2026-04-01T10:00:00Z"
        "2026-04-03T10:00:00Z"
        "2026-04-05T10:00:00Z"
        "2026-04-07T10:00:00Z"
        "2026-04-09T10:00:00Z"
    )
    local -a featured=(true true true false false)

    for i in "${!titles[@]}"; do
        actualite_ids+=("$(create_actualite "$token" "${titles[$i]}" "${contents[$i]}" "${dates[$i]}" "${featured[$i]}" "${photo_files[$i]}")")
    done

    printf "%s\n" "${actualite_ids[@]}"
}

seed_researchers() {
    local token="$1"
    shift
    local -a domain_ids=("$@")
    local -a researcher_ids=()

    local -a photo_files=(
        "$RESOURCE_DIR/researchers/jane.jpg"
        "$RESOURCE_DIR/researchers/john.jpg"
        "$RESOURCE_DIR/researchers/sam.jpg"
    )
    local -a names=(
        "Jane Doe"
        "John Smith"
        "Sam Taylor"
    )
    local -a bios=(
        "AI researcher focused on trustworthy systems and model evaluation."
        "Quantum computing researcher working on algorithms and tooling."
        "Research scientist in data-driven health and sustainability projects."
    )
    local -a domain_sets=(
        "[0,1]"
        "[3,5]"
        "[2,6,7]"
    )

    for i in "${!names[@]}"; do
        local domain_ids_json
        domain_ids_json=$(jq -nc --argjson domains "$(json_array "${domain_ids[@]}")" --argjson idxs "${domain_sets[$i]}" '$idxs | map($domains[.])')
        researcher_ids+=("$(create_researcher "$token" "${names[$i]}" "${bios[$i]}" "$domain_ids_json" "${photo_files[$i]}")")
    done

    printf "%s\n" "${researcher_ids[@]}"
}

seed_publications() {
    local token="$1"
    shift
    local -a researcher_ids=("$@")
    local -a publication_ids=()

    local -a pdf_files=(
        "$RESOURCE_DIR/publications/Artificial intelligence in disease diagnosis_ Application within.pdf"
        "$RESOURCE_DIR/publications/Assessing Physicians’ Readiness for Medical Artificial.pdf"
        "$RESOURCE_DIR/publications/From Quantum Computing Algorithms to Human Health applications.pdf"
        "$RESOURCE_DIR/publications/Scalable Architectures for quantum computing.pdf"
        "$RESOURCE_DIR/publications/The Global E-waste.pdf"
        "$RESOURCE_DIR/publications/research_overview.pdf"
    )
    local -a titles=(
        "AI in Disease Diagnosis"
        "Physicians' Readiness for Medical AI"
        "Quantum Algorithms for Human Health"
        "Scalable Quantum Architectures"
        "The Global E-waste Challenge"
        "Research Overview 2026"
    )
    local -a resumes=(
        "An applied study on artificial intelligence for disease diagnosis."
        "An assessment of physician preparedness for medical AI deployment."
        "A cross-disciplinary look at quantum computing algorithms and health use cases."
        "Design principles for scalable quantum computing systems."
        "Analysis of global e-waste trends and sustainability implications."
        "A consolidated overview of current research themes and outcomes."
    )
    local -a dois=(
        "10.1000/techcenter.ai.diag"
        "10.1000/techcenter.med.ai"
        "10.1000/techcenter.quantum.health"
        "10.1000/techcenter.quantum.arch"
        "10.1000/techcenter.e-waste"
        "10.1000/techcenter.overview"
    )
    local -a dates=(
        "2026-03-01T10:00:00Z"
        "2026-03-05T10:00:00Z"
        "2026-03-10T10:00:00Z"
        "2026-03-15T10:00:00Z"
        "2026-03-20T10:00:00Z"
        "2026-03-25T10:00:00Z"
    )
    local -a researcher_sets=(
        "[0]"
        "[0,1]"
        "[1]"
        "[1,2]"
        "[2]"
        "[0,2]"
    )

    for i in "${!titles[@]}"; do
        local researcher_ids_json
        researcher_ids_json=$(jq -nc --argjson researchers "$(json_array "${researcher_ids[@]}")" --argjson idxs "${researcher_sets[$i]}" '$idxs | map($researchers[.])')
        publication_ids+=("$(create_publication "$token" "${titles[$i]}" "${resumes[$i]}" "${dois[$i]}" "${dates[$i]}" "$researcher_ids_json" "${pdf_files[$i]}")")
    done

    printf "%s\n" "${publication_ids[@]}"
}

main() {
    require_cmd curl
    require_cmd jq
    require_cmd psql

    setup_user ADMIN "$ADMIN_EMAIL" "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN"
    setup_user MODERATOR "$MODERATOR_EMAIL" "$MODERATOR_USERNAME" "$MODERATOR_PASSWORD" "MODERATOR"
    setup_user USER "$USER_EMAIL" "$USER_USERNAME" "$USER_PASSWORD" "USER"

    mapfile -t DOMAIN_IDS < <(seed_domains "$ADMIN_TOKEN")
    mapfile -t ACTUALITE_IDS < <(seed_actualites "$MODERATOR_TOKEN")
    mapfile -t RESEARCHER_IDS < <(seed_researchers "$ADMIN_TOKEN" "${DOMAIN_IDS[@]}")
    mapfile -t PUBLICATION_IDS < <(seed_publications "$ADMIN_TOKEN" "${RESEARCHER_IDS[@]}")

    echo
    echo "Seeded data counts: domains=${#DOMAIN_IDS[@]}, actualites=${#ACTUALITE_IDS[@]}, researchers=${#RESEARCHER_IDS[@]}, publications=${#PUBLICATION_IDS[@]}"
    echo
    echo "Users are ready. Tokens for quick testing:"
    echo "export ADMIN_TOKEN='$ADMIN_TOKEN'"
    echo "export MODERATOR_TOKEN='$MODERATOR_TOKEN'"
    echo "export USER_TOKEN='$USER_TOKEN'"
}

main "$@"
