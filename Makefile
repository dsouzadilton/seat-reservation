burst:
	python scripts/burst.py $(BASE_URL) --users $(or $(USERS),500) --concurrency $(or $(CONCURRENCY),500) --mode $(or $(MODE),both) --seats $(or $(SEATS),5000)