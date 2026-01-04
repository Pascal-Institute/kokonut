# AGENTS.md

This document outlines the development philosophy and coding standards for our agents. All contributors are expected to adhere to these principles to ensure the longevity and reliability of the system.

---

## 1. Development Philosophy

### **Intuitive & Clear Coding**
Code is written for humans to read and machines to execute. We prioritize clarity over cleverness.

* **Predictable Logic:** Write code that follows common patterns. Avoid "magic" numbers or hidden side effects that might surprise another developer.
* **Purposeful Naming:** Use descriptive names for variables, functions, and classes. A name should reveal intent (e.g., use `validateTransactionSignature` instead of `checkSig`).
* **Simplicity:** If a solution feels overly complex, it probably is. Aim for the simplest implementation that satisfies the requirement.

### **Maintainability & Readability**
Our goal is to build software that is easy to evolve and debt-free.

* **Modular Architecture:** Break down complex logic into small, reusable, and independent modules. This limits the "blast radius" of changes.
* **Self-Documenting Code:** Prioritize writing code that explains itself. Use comments primarily to explain the **"Why"** (the reasoning behind a decision) rather than the **"What"** (which should be clear from the code).
* **Consistency:** Follow the established project structure and linting rules. Consistent formatting reduces cognitive load during code reviews.

---

## 2. Kotlin Specific Standards

To maximize the benefits of Kotlin, follow these specific guidelines:

* **Null Safety:** Avoid using `!!` (double bang). Use safe calls (`?.`), the Elvis operator (`?:`), or `requireNotNull()` to handle nullability explicitly.
* **Immutability:** Favor `val` over `var`. Use immutable collections (`listOf`, `mapOf`) to prevent unintended state changes.
* **Coroutines for Asynchrony:** Use structured concurrency with Kotlin Coroutines for non-blocking operations. Ensure `CoroutineScope` is managed properly to avoid memory leaks.
* **Expressive Syntax:** Utilize `data class` for models, `sealed class` for restricted hierarchies (like transaction states), and extension functions to keep logic clean and readable.

---

## 3. Core Blockchain Principles

The integrity of our system rests upon three foundational pillars. Every line of code related to the ledger must uphold these standards:



### **Integrity**
> **Unalterable Accuracy.** We ensure that data remains precise and untampered with from the moment of its creation. By implementing rigorous cryptographic hashing and decentralized consensus, we guarantee a "single version of truth" that is immune to unauthorized modification.

### **Connectivity**
> **Chronological Linkage.** No data exists in isolation. Each block is intrinsically linked to its predecessor through a cryptographic chain. This seamless connectivity ensures total traceability, allowing anyone to verify the entire history of the ledger back to the genesis block.

### **Stability**
> **Resilient Architecture.** The system must remain operational and secure under any circumstances. Through robust error handling (using Kotlin's `Result` or `try-catch`), fault tolerance, and decentralized node synchronization, we protect the network against systemic failures and malicious attacks.

---

## 4. Agent Implementation Checklist

Before submitting a Pull Request, ensure your agent code answers **"Yes"** to the following:

* [ ] **Is it readable?** Could a new developer understand this logic in 5 minutes?
* [ ] **Is it modular?** Does each function serve a single, clear responsibility?
* [ ] **Does it respect Integrity?** Are there checks to prevent data tampering?
* [ ] **Is it stable?** Are edge cases and network delays handled gracefully?
* [ ] **Is it Idiomatic Kotlin?** Does it avoid Java-isms and use Kotlin's native features correctly?
