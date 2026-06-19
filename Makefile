## ledger-bank — common developer commands.
## Run `make help` for the list.

CORE_BANK := core-bank
GRADLEW   := ./gradlew

.DEFAULT_GOAL := help

.PHONY: help up down restart logs ps build test fmt lint seed clean tools

help: ## Show this help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

up: ## Build and start the full stack (Postgres + core-bank).
	docker compose up --build -d

tools: ## Start optional tooling (pgAdmin) alongside the stack.
	docker compose --profile tools up -d

down: ## Stop the stack and remove containers.
	docker compose down

restart: down up ## Restart the stack.

logs: ## Tail logs from all services.
	docker compose logs -f

ps: ## Show running services.
	docker compose ps

build: ## Build the core-bank application jar.
	cd $(CORE_BANK) && $(GRADLEW) clean bootJar

test: ## Run the core-bank test suite (Testcontainers; Docker required).
	cd $(CORE_BANK) && $(GRADLEW) test

fmt: ## Format code (wired up in a later phase).
	@echo "fmt: not configured yet (Spotless lands in Phase 5)."

lint: ## Static analysis (wired up in a later phase).
	@echo "lint: not configured yet (Checkstyle/SpotBugs land in Phase 5)."

seed: ## Seed demo data (lands with Phase 1 money operations).
	@echo "seed: not configured yet (arrives in Phase 1)."

clean: ## Stop the stack and delete volumes (DESTROYS local data).
	docker compose down -v
