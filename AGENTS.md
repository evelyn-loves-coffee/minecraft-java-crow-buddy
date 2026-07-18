# AGENTS.md

## 1. Session Initialization
* **Sync Check:** Perform `git fetch` immediately.
* **Auto-Rebase:** If a desync is detected, automatically execute `git rebase`. Stop and request user intervention **only** if merge conflicts occur.

## 2. Operational Phases
Apply the **PAWS** standard during these phases. All phases must maintain a "Golden Thread" of evidence from discovery to implementation.

### A. Analysis (Discovery)
* Identify requirements and constraints.
* **Requirement:** Every technical assumption must be **Auditable** (linked to empirical evidence such as documentation, benchmarks, or observed system behavior).

### B. Design (Architectural Planning)
* **High-Level Design (HLD):** Define major components and subsystem interactions.
* **Low-Level Design (LLD):** Detail specific implementation logic. The LLD must be a **Workable** derivation of the HLD, ensuring the task breakdown is technically viable and verifiable.

### C. Review (Implementation Verification)
* **Compliance Check:** Verify code matches requirements (Analysis), structure (HLD), and logic (LLD).
* **Empirical Verification:** Confirm implementation matches design via complexity analysis, test results, and runtime metrics.

## 3. The PAWS Framework
All evaluations must be measured against these four pillars:

| Priority | Criterion | Key Rule |
| :--- | :--- | :--- |
| 1 | **P**erformance | Analyze Big O; identify bottlenecks; minimize memory/time complexity. |
| 2 | **A**uditability | Ensure input validation, least privilege, traceable logging, and **traceability of assumptions to empirical data.** |
| 3 | **W**orkability | Verify edge-cases, idempotency, thread safety, and **empirical viability (proven via tests/metrics).** |
| 4 | **S**calability | Prioritize modularity, DRY principles, and separation of concerns. |

## 4. Reporting & Output Standards
Every evaluation must conclude with a per-agent report:
1. **Risk Assessment:** Technical, security, or logical risks.
2. **Confidence Level:** Degree of certainty in findings/implementation.
3. **Path to Improvement:** Actionable optimization steps.
4. **Intent Adherence:** Confirmation of fulfillment of original goals.

### Risk & Open Question Protocol
When identifying risks or open questions, you **must** provide:
* **Proposed Solutions:** Multiple distinct paths forward.
* **Comparative Analysis:** A **tabular comparison** of the pros and cons for each solution.
* **Recommended Path:** A definitive, justified recommendation based on **PAWS** criteria.
