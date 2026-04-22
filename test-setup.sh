#!/usr/bin/env bash

set -euo pipefail

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

RESEARCHER_EMAIL="${RESEARCHER_EMAIL:-researcher.test@example.com}"
RESEARCHER_USERNAME="${RESEARCHER_USERNAME:-researcher_test}"
RESEARCHER_PASSWORD="${RESEARCHER_PASSWORD:-StrongPass123!}"

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

main() {
    require_cmd curl
    require_cmd jq
    require_cmd psql

    setup_user ADMIN "$ADMIN_EMAIL" "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "ADMIN"
    setup_user MODERATOR "$MODERATOR_EMAIL" "$MODERATOR_USERNAME" "$MODERATOR_PASSWORD" "MODERATOR"
    setup_user RESEARCHER "$RESEARCHER_EMAIL" "$RESEARCHER_USERNAME" "$RESEARCHER_PASSWORD" "RESEARCHER"
    setup_user USER "$USER_EMAIL" "$USER_USERNAME" "$USER_PASSWORD" "USER"

    echo
    echo "Users are ready. Tokens for quick testing:"
    echo "export ADMIN_TOKEN='$ADMIN_TOKEN'"
    echo "export MODERATOR_TOKEN='$MODERATOR_TOKEN'"
    echo "export RESEARCHER_TOKEN='$RESEARCHER_TOKEN'"
    echo "export USER_TOKEN='$USER_TOKEN'"
}

main "$@"
