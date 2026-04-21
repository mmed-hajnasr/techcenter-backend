help: ## Show this help message
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start infra only (Postgres/Redis/Jaeger)
	docker-compose up -d

down: ## Stop the development environment
	docker-compose down

clean: ## Stop and remove all containers, networks, and volumes
	docker-compose down -v

clean-start: ## Clean and start the development environment
	$(MAKE) clean
	$(MAKE) up

prune: ## Remove unused Docker resources
	docker system prune -f
