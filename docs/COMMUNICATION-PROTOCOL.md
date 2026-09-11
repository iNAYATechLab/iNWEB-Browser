# iNWEB Browser — Step-Completion Communication Protocol

**Status:** binding.
**Scope:** how the development agent reports completed steps to the product owner.

## Source and amendment history

| Date | Event |
|---|---|
| 2026-09-12 | Base format adopted from `MASTER-SPEC.md` §61 |
| 2026-09-12 | **User-directed amendment:** the three continuation commands are rendered as **three separate code blocks** (one per command), superseding the "same code block" rendering detail of §61. All other §61 rules remain in force |

## Requirements

1. Step-completion communication with the user is in **Bangla**.
2. After each development step, use the compact structure below — no long
   technical status reports, no separate issue-count reports, no graphical
   "step completion buttons".
3. Completed work is listed with **short names of the completed items** only.
4. The agent performs all build / test / defect validations itself and never
   requires the user to report build status, test status, issue counts,
   completion percentages, or technical validation details.
5. A serious blocker is stated clearly in Bangla **before** the next-action
   proposal (§56, §61).
6. All artifacts (code, docs, commits) remain in professional English; only
   user-facing step communication is Bangla.

## Exact rendered format

```text
বস, আমি (স্টেপ সংখ্যা) নং স্টেপে নিচের কাজগুলো সম্পন্ন করেছি।
• (short name of completed item)
• (short name of completed item)
• (short name of completed item)
• (short name of completed item)
```

followed by the next-step proposal:

```text
বস, পরবর্তী ধাপে আমি (স্টেপ নং) (কাজের বিবরণ) করবো।
```

followed by the three continuation commands, **each in its own code block**:

```text
• হ্যাঁ ঠিক আছে।
```

```text
• পরবর্তী কার্যক্রম শুরু করো।
```

```text
• এখন না পরে করবো, (অন্য কাজের নাম) এটা করো এখন।
```

closing with:

```text
ধন্যবাদ বস
```

## Continuation command semantics (§62)

| Command | Meaning |
|---|---|
| `হ্যাঁ ঠিক আছে।` | Approval to continue with the proposed next step |
| `পরবর্তী কার্যক্রম শুরু করো।` | Instruction to continue with the proposed next step |
| `এখন না পরে করবো, (অন্য কাজের নাম) এটা করো এখন।` | Defer the proposed step; execute the named alternative (if compatible with the master architecture; otherwise explain and select the closest valid alternative per §63) |
