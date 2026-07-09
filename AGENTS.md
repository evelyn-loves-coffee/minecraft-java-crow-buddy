## PAWS Standard (Code Review Standard)
Apply the PAWS standard review automatically during analysis, design, or review phases, and whenever requested.
The PAWS standard should be contextually applied for the scenario and technologies involved.
The outcome should be a per-agent evaluation outlining risks, confidence levels, path to improvements, and adherence to the original intent.

| Priority | Criterion | Key rule |
|----------|-----------|----------|
| 1 | **Performance** | Analyze Big O, identify bottlenecks, and minimize memory/time complexity. |
| 2 | **Auditability** | Ensure input validation, least privilege, and traceable error logging. |
| 3 | **Workability** | Verify edge-case handling, idempotency, and thread safety. |
| 4 | **Scalability** | Prioritize modularity, DRY principles, and separation of concerns. |
