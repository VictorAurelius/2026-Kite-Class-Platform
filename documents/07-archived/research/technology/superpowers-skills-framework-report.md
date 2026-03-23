# Superpowers - Agentic Skills Framework

**Repository:** https://github.com/obra/superpowers
**License:** MIT License
**Primary Language:** Shell (54.8%)
**Stars:** 80,544 ⭐
**Forks:** 6,242
**Created:** 2025-10-09
**Last Updated:** 2026-03-13
**Status:** Actively Maintained ✅

---

## 📋 Tổng Quan

Superpowers là một **framework agentic skills** và **phương pháp phát triển phần mềm** hoàn chỉnh được thiết kế để nâng cao khả năng của AI coding agents (như Claude Code, Cursor, Codex) thông qua các quy trình có cấu trúc cho việc lập kế hoạch, triển khai và đánh giá code một cách có hệ thống.

### Định nghĩa chính

> "A complete software development workflow for your coding agents, built on top of a set of composable skills."

Framework này **không phải là suggestions** - đây là **mandatory workflows** được kích hoạt tự động khi agent phát hiện task phù hợp.

---

## 🎯 Mục Đích & Giá Trị

### Vấn đề giải quyết

1. **Thiếu quy trình có hệ thống:** AI agents thường code ngay lập tức mà không có planning
2. **Test-driven development không nhất quán:** Không tuân thủ RED-GREEN-REFACTOR
3. **Code review không chuẩn:** Thiếu checklist và quy trình review
4. **Debugging ad-hoc:** Không có phương pháp systematic để tìm root cause
5. **Parallel work không hiệu quả:** Thiếu coordination giữa subagents

### Giá trị mang lại

- ✅ **Systematic over Ad-hoc:** Quy trình có cấu trúc thay vì làm ngẫu nhiên
- ✅ **Test-Driven Development:** Bắt buộc viết test trước khi implement
- ✅ **Complexity Reduction:** Đơn giản hóa là mục tiêu chính
- ✅ **Evidence-Based Verification:** Xác minh trước khi hoàn thành
- ✅ **Parallel Subagents:** Dispatch nhiều agents cùng lúc với 2-stage review

---

## 🛠️ Tech Stack

### Ngôn ngữ sử dụng

| Language | Percentage | Size (bytes) | Purpose |
|----------|------------|--------------|---------|
| Shell | 54.8% | 87,952 | Core scripts & automation |
| JavaScript | 32.1% | 51,390 | Skills implementation |
| Python | 4.2% | 6,733 | Support utilities |
| TypeScript | 3.2% | 5,054 | Type-safe components |
| HTML | 4.8% | 7,703 | Documentation |
| Batchfile | 0.9% | 1,460 | Windows support |

### Dependencies

- **Git:** Version control & worktrees management
- **Compatible AI Agent:** Claude Code, Cursor, Codex, OpenCode, or Gemini CLI
- **Shell Environment:** Bash/Zsh for script execution

---

## 📚 Available Skills

Superpowers tổ chức các skills thành 4 categories:

### 1️⃣ Testing Skills

**`test-driven-development`**
- Implements RED-GREEN-REFACTOR cycles
- Anti-patterns reference
- Deletes code written before tests pass
- Enforces TDD methodology

### 2️⃣ Debugging Skills

**`systematic-debugging`**
- Four-phase root cause analysis:
  1. Reproduce the issue
  2. Trace execution flow
  3. Identify root cause
  4. Implement defensive fixes
- Evidence-based verification

**`verification-before-completion`**
- Confirms fixes are actually working
- Prevents false completion claims

### 3️⃣ Collaboration Skills

**`brainstorming`**
- Socratic-method design refinement
- Explores alternatives through questions
- Documents design decisions

**`writing-plans`**
- Creates detailed implementation roadmaps
- Breaks work into 2-5 minute tasks
- Exact file paths & code samples
- Verification steps for each task

**`executing-plans`**
- Batch execution with human checkpoints
- Progress tracking
- Rollback on failures

**`dispatching-parallel-agents`**
- Enables concurrent subagent workflows
- Load balancing across agents
- Coordination & communication

**`requesting-code-review`**
- Pre-review quality checklist
- Self-assessment before review
- Documentation completeness check

**`receiving-code-review`**
- Handles feedback integration
- Prioritizes issues by severity
- Blocks on critical issues

**`using-git-worktrees`**
- Manages parallel development branches
- Isolated workspaces
- Clean separation of features

**`finishing-a-development-branch`**
- Guides merge/PR decisions
- Cleanup worktrees
- Verifies tests pass

**`subagent-driven-development`**
- Fast iteration with two-stage review:
  1. **Spec compliance review:** Does it match requirements?
  2. **Code quality review:** Is the code clean & maintainable?

### 4️⃣ Meta Skills

**`writing-skills`**
- Creates new skills following best practices
- Templates & guidelines
- Community contribution workflow

**`using-superpowers`**
- System introduction & onboarding
- How skills work together
- Usage examples

---

## 🔄 Workflow Methodology

Superpowers implements a **7-stage sequential process:**

```
┌─────────────────┐
│ 1. Brainstorming│ → Refines ideas through questions
└────────┬────────┘
         ↓
┌─────────────────┐
│ 2. Git Worktrees│ → Creates isolated development branches
└────────┬────────┘
         ↓
┌─────────────────┐
│ 3. Plan Writing │ → Breaks work into bite-sized tasks
└────────┬────────┘
         ↓
┌─────────────────┐
│ 4. Subagent Dev │ → Dispatches parallel agents with 2-stage review
└────────┬────────┘
         ↓
┌─────────────────┐
│ 5. TDD Cycles   │ → RED-GREEN-REFACTOR enforcement
└────────┬────────┘
         ↓
┌─────────────────┐
│ 6. Code Review  │ → Reviews against plan, blocks on critical issues
└────────┬────────┘
         ↓
┌─────────────────┐
│ 7. Branch Close │ → Verifies tests, merges, cleans up
└─────────────────┘
```

### Key Principles

1. **Automatic Skill Activation:** Skills trigger automatically without explicit commands
2. **Mandatory Workflows:** Not optional suggestions - agents MUST follow them
3. **Question Before Code:** Clarify intent through dialogue first
4. **Evidence-Based:** Validate before declaring success
5. **Simplicity First:** Complexity reduction is the primary objective

---

## 📦 Installation

### Claude Code (Official Marketplace)

```bash
/plugin install superpowers@claude-plugins-official
```

### Claude Code (Custom Marketplace)

```bash
/plugin marketplace add obra/superpowers-marketplace
/plugin install superpowers@superpowers-marketplace
```

### Cursor Agent Chat

- Search "superpowers" in plugin marketplace
- OR use: `/add-plugin superpowers`

### Codex

```
Tell Codex: "Fetch and follow instructions from https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.codex/INSTALL.md"
```

### OpenCode

```
Tell OpenCode: "Fetch and follow instructions from https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.opencode/INSTALL.md"
```

### Gemini CLI

```bash
gemini extensions install https://github.com/obra/superpowers
gemini extensions update superpowers
```

### Verification

After installation, verify by requesting:
- "Help me plan this feature" (triggers brainstorming + planning)
- "Let's debug this issue" (triggers systematic debugging)

---

## 💡 Usage Examples

### Example 1: Feature Planning

**User Request:**
```
"Help me plan this feature: Add user authentication to the app"
```

**Automatic Activation:**
1. ✅ `brainstorming` skill triggers
   - Asks questions about auth method (JWT, session, OAuth?)
   - Explores security requirements
   - Documents design decisions

2. ✅ `writing-plans` skill triggers
   - Creates implementation roadmap
   - Breaks into tasks (e.g., "Create User entity - 3 min")
   - Specifies exact files & code samples

3. ✅ `using-git-worktrees` skill triggers
   - Creates isolated branch: `feature/user-authentication`
   - Verifies clean test baseline

### Example 2: Debugging

**User Request:**
```
"Let's debug this login failure issue"
```

**Automatic Activation:**
1. ✅ `systematic-debugging` skill triggers
   - **Phase 1:** Reproduce the issue
   - **Phase 2:** Trace execution flow
   - **Phase 3:** Identify root cause
   - **Phase 4:** Implement defensive fixes

2. ✅ `verification-before-completion` skill triggers
   - Confirms fix actually works
   - Runs regression tests

### Example 3: Subagent Development

**Agent Workflow:**
1. ✅ Plan approved by user
2. ✅ `dispatching-parallel-agents` activates
3. ✅ Separate agents work on each task concurrently
4. ✅ **Two-stage review per task:**
   - Stage 1: Spec compliance (matches requirements?)
   - Stage 2: Code quality (clean & maintainable?)
5. ✅ Claude works independently for hours following established plan

---

## 🏗️ Architecture & Design

### Core Design Principles

1. **Composable Skills:** Mỗi skill làm 1 việc tốt, kết hợp được với nhau
2. **Contextual Activation:** Skills tự trigger dựa trên context, không cần user command
3. **Mandatory Enforcement:** Không phải suggestions - agents PHẢI follow
4. **Two-Stage Review:** Spec compliance trước, code quality sau
5. **Worktree Isolation:** Mỗi feature có branch riêng biệt

### Skill Anatomy

Mỗi skill bao gồm:
- **Trigger Conditions:** Khi nào skill được activate
- **Process Steps:** Các bước thực hiện cụ thể
- **Exit Criteria:** Khi nào skill hoàn thành
- **Integration Points:** Skills nào có thể chạy tiếp theo

### Comparison với Traditional Workflows

| Aspect | Traditional Agent | Superpowers Agent |
|--------|-------------------|-------------------|
| Planning | Code immediately | Brainstorm → Plan → Approve |
| Testing | Write tests after (maybe) | **RED-GREEN-REFACTOR mandatory** |
| Branching | Manual git commands | **Automatic worktrees** |
| Review | Manual checklist | **Automated pre-review + 2-stage** |
| Debugging | Trial & error | **Systematic 4-phase process** |
| Parallel Work | Not supported | **Subagent dispatch with coordination** |

---

## ✅ Ưu Điểm

### 1. Systematic Development Process
- ✅ Không code ngay lập tức - brainstorm & plan trước
- ✅ Quy trình có cấu trúc, lặp lại được
- ✅ Evidence-based verification

### 2. Test-Driven Development Enforcement
- ✅ Bắt buộc RED-GREEN-REFACTOR
- ✅ Delete code written before tests pass
- ✅ Anti-patterns reference

### 3. Git Workflow Integration
- ✅ Automatic worktrees cho parallel development
- ✅ Isolated branches per feature
- ✅ Clean merge/PR guidance

### 4. Code Review Automation
- ✅ Pre-review quality checklist
- ✅ Two-stage review (spec + quality)
- ✅ Block on critical issues

### 5. Scalability với Subagents
- ✅ Parallel agent dispatch
- ✅ Coordination & load balancing
- ✅ Agents work independently for hours

### 6. Active Community
- ✅ 80.5k stars - strong adoption
- ✅ 6.2k forks - community contributions
- ✅ MIT License - open source

---

## ⚠️ Nhược Điểm & Limitations

### 1. Platform Dependency
- ❌ Chỉ hoạt động với AI agents cụ thể (Claude, Cursor, Codex, etc.)
- ❌ Không thể dùng standalone
- ❌ Installation khác nhau cho mỗi platform

### 2. Mandatory Workflows
- ⚠️ Skills bắt buộc, không thể skip
- ⚠️ Có thể làm chậm cho simple tasks
- ⚠️ Overhead cho quick fixes

### 3. Learning Curve
- ❌ User cần hiểu workflow methodology
- ❌ Không rõ cách customize skills
- ❌ Debug skill failures có thể khó

### 4. Limited Documentation
- ⚠️ README không explicit về limitations
- ⚠️ Thiếu troubleshooting guide
- ⚠️ Không có performance benchmarks

### 5. Shell-Heavy Codebase
- ❌ 54.8% Shell code - khó maintain & test
- ❌ Cross-platform compatibility concerns (Windows?)
- ❌ Debugging shell scripts phức tạp

---

## 📊 So Sánh Với Alternatives

### Superpowers vs Traditional Git Hooks

| Feature | Git Hooks | Superpowers |
|---------|-----------|-------------|
| Automation | Pre/post commit only | Full workflow automation |
| Planning | None | Brainstorming + Plan writing |
| TDD | Optional | **Mandatory RED-GREEN-REFACTOR** |
| Code Review | Manual | **Automated 2-stage review** |
| Debugging | Manual | **Systematic 4-phase process** |
| Parallel Work | Not supported | **Subagent dispatch** |

### Superpowers vs GitHub Actions

| Feature | GitHub Actions | Superpowers |
|---------|----------------|-------------|
| Scope | CI/CD pipeline | **Full development workflow** |
| Activation | Push/PR events | **Contextual agent detection** |
| Planning | None | **Brainstorming + detailed plans** |
| TDD | Manual setup | **Built-in enforcement** |
| Local Dev | Limited | **Primary use case** |
| Agent Integration | No | **Native support** |

### Superpowers vs .claude/skills (Kite Class Platform)

| Feature | Kite Class `.claude/skills/` | Superpowers |
|---------|------------------------------|-------------|
| Purpose | Project-specific guidelines | **Generic framework** |
| Scope | Code quality, conventions | **Full dev lifecycle** |
| Activation | Manual reading | **Automatic triggering** |
| TDD | Guidelines only | **Enforced workflow** |
| Subagents | Not supported | **Built-in dispatch** |
| Customization | Easy (markdown files) | Requires skill writing |
| Platform | Claude Code specific | **Multi-platform support** |

---

## 🎯 Applicability to Kite Class Platform

### Potential Use Cases

#### 1️⃣ Systematic Feature Development ✅ HIGH VALUE
- **Current:** Manual planning trong implementation-plan.md
- **With Superpowers:** Automatic brainstorming → detailed plans → subagent execution
- **Benefit:** Faster iteration, consistent planning structure

#### 2️⃣ Test-Driven Development Enforcement ✅ MEDIUM VALUE
- **Current:** TDD encouraged nhưng không bắt buộc
- **With Superpowers:** RED-GREEN-REFACTOR mandatory
- **Benefit:** Higher test coverage, fewer regressions

#### 3️⃣ Code Review Automation ✅ MEDIUM VALUE
- **Current:** Manual review checklist trong skills/code-review.md
- **With Superpowers:** Pre-review checklist + 2-stage review
- **Benefit:** Consistent review quality

#### 4️⃣ Debugging Systematic Process ✅ LOW-MEDIUM VALUE
- **Current:** Ad-hoc debugging
- **With Superpowers:** 4-phase systematic process
- **Benefit:** Faster root cause identification

#### 5️⃣ Git Worktrees for Parallel Development ✅ LOW VALUE
- **Current:** Feature branch workflow đã tốt
- **With Superpowers:** Automatic worktrees
- **Benefit:** Marginal - current workflow already uses branches

### Potential Challenges

#### 1. Integration với Existing Workflow ⚠️
- Kite Class đã có `.claude/skills/` với detailed guidelines
- Superpowers có thể conflict với existing rules
- Cần reconcile 2 systems

#### 2. Mandatory Workflows có thể quá Rigid ⚠️
- Hotfixes/urgent bugs cần fast-track
- Superpowers bắt buộc full workflow (brainstorm → plan → TDD)
- Có thể làm chậm emergency fixes

#### 3. Shell-Heavy Implementation ⚠️
- Kite Class là Java/TypeScript project
- Superpowers 54.8% Shell code
- Cross-platform concerns (Windows dev environments?)

#### 4. Learning Curve cho Team ⚠️
- Team cần học workflow methodology
- Customize skills requires technical knowledge
- Onboarding time investment

### Recommendations

#### ✅ RECOMMEND: Adopt Selected Skills
Không cần install full Superpowers - có thể **học concepts và adapt vào `.claude/skills/`:**

1. **Adopt `systematic-debugging` methodology**
   - Tạo `.claude/skills/systematic-debugging.md` với 4-phase process
   - Bắt buộc cho mọi bug investigation

2. **Adopt `writing-plans` structure**
   - Enhance implementation-plan.md với:
     - 2-5 minute task breakdown
     - Exact file paths
     - Code samples
     - Verification steps

3. **Adopt `subagent-driven-development` review process**
   - 2-stage review: spec compliance → code quality
   - Document trong skills/code-review.md

4. **Adopt `test-driven-development` enforcement**
   - Add pre-commit hook để check RED-GREEN-REFACTOR
   - Reject commits without tests

#### ❌ NOT RECOMMEND: Full Installation
- Existing `.claude/skills/` đã cover nhiều use cases
- Shell-heavy implementation không fit với Java/TS stack
- Mandatory workflows quá rigid cho current needs
- Better to cherry-pick concepts

---

## 🔬 Technical Deep Dive

### Skill Execution Model

```javascript
// Conceptual model - simplified
class SkillFramework {
  async detectRelevantSkills(context) {
    const skills = [];

    // Check for planning keywords
    if (context.includes('plan') || context.includes('feature')) {
      skills.push('brainstorming', 'writing-plans');
    }

    // Check for debugging keywords
    if (context.includes('debug') || context.includes('bug')) {
      skills.push('systematic-debugging');
    }

    // Check for code review request
    if (context.includes('review')) {
      skills.push('requesting-code-review');
    }

    return skills;
  }

  async executeSkill(skillName, context) {
    const skill = this.loadSkill(skillName);

    // Mandatory execution - cannot skip
    if (!skill.canSkip()) {
      return await skill.execute(context);
    }
  }
}
```

### Git Worktrees Management

```bash
# Conceptual implementation
create_worktree() {
  local branch_name=$1
  local worktree_path="./worktrees/$branch_name"

  # Create worktree with new branch
  git worktree add "$worktree_path" -b "$branch_name"

  # Setup project in worktree
  cd "$worktree_path"
  npm install  # or ./mvnw clean install for Java

  # Verify clean test baseline
  npm test || ./mvnw test

  echo "✅ Worktree ready at $worktree_path"
}

cleanup_worktree() {
  local branch_name=$1
  local worktree_path="./worktrees/$branch_name"

  # Remove worktree
  git worktree remove "$worktree_path"

  # Delete branch if merged
  git branch -d "$branch_name"
}
```

### Two-Stage Review Process

```javascript
// Conceptual review flow
async function reviewTask(task, implementation) {
  // Stage 1: Spec Compliance Review
  const specReview = await reviewSpecCompliance(task, implementation);

  if (!specReview.passed) {
    return {
      stage: 'spec',
      issues: specReview.issues,
      action: 'BLOCK - Fix spec compliance first'
    };
  }

  // Stage 2: Code Quality Review
  const qualityReview = await reviewCodeQuality(implementation);

  return {
    stage: 'quality',
    specPassed: true,
    qualityIssues: qualityReview.issues,
    action: qualityReview.critical.length > 0 ? 'BLOCK' : 'APPROVE'
  };
}

async function reviewSpecCompliance(task, implementation) {
  return {
    passed: implementation.matchesRequirements(task.spec),
    issues: [
      // Does it do what the task asked for?
      // Are all acceptance criteria met?
      // Does it match the exact file paths specified?
    ]
  };
}

async function reviewCodeQuality(implementation) {
  return {
    critical: [
      // Security vulnerabilities
      // Data loss risks
      // Breaking changes
    ],
    major: [
      // Performance issues
      // Maintainability concerns
      // Test coverage gaps
    ],
    minor: [
      // Style inconsistencies
      // Minor refactoring opportunities
    ]
  };
}
```

---

## 📈 Project Statistics & Community

### GitHub Metrics (as of 2026-03-13)

- **Stars:** 80,544 ⭐ (exceptionally high adoption)
- **Forks:** 6,242 (strong community engagement)
- **Commits:** 355+ commits
- **Contributors:** Multiple active contributors
- **License:** MIT (permissive open source)
- **Last Updated:** 2026-03-13 (actively maintained)

### Activity Indicators

✅ **High Activity:**
- Created Oct 2025, reached 80k stars in 5 months
- Consistent updates (last update today)
- Strong fork count indicates community contributions

✅ **Production Ready:**
- Multi-platform support (Claude, Cursor, Codex, Gemini)
- Documented installation & usage
- Clear methodology & principles

---

## 🚀 Future Considerations

### Potential Enhancements

1. **Visual Workflow Dashboard**
   - Real-time progress tracking
   - Skill execution visualization
   - Subagent coordination monitoring

2. **Custom Skill Templates**
   - Easier skill creation for domain-specific workflows
   - Template library for common patterns
   - Skill marketplace

3. **Performance Metrics**
   - Time saved vs manual workflows
   - Code quality improvements
   - Bug reduction statistics

4. **IDE Integrations**
   - VSCode extension
   - IntelliJ plugin
   - WebStorm support

5. **Language Support**
   - Reduce Shell dependency
   - TypeScript/JavaScript rewrite for better maintainability
   - Cross-platform improvements

---

## 📖 References & Resources

### Official Links
- **GitHub Repository:** https://github.com/obra/superpowers
- **Installation Guides:**
  - Codex: https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.codex/INSTALL.md
  - OpenCode: https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.opencode/INSTALL.md

### Related Concepts
- **Test-Driven Development (TDD):** RED-GREEN-REFACTOR methodology
- **Agentic Workflows:** AI agents with structured processes
- **Git Worktrees:** Parallel development branches management
- **Code Review Best Practices:** Two-stage review methodology

### Similar Projects
- **GitHub Copilot Workspace:** IDE-integrated AI coding
- **Cursor Rules:** Editor-specific AI guidelines
- **.claude/skills/:** Project-specific agent instructions (Kite Class approach)

---

## 💡 Key Takeaways

### For Kite Class Platform

1. ✅ **Systematic Planning is Valuable:** Adopt brainstorming → detailed plans structure
2. ✅ **TDD Enforcement Works:** Consider pre-commit hooks for test requirements
3. ✅ **Two-Stage Review is Better:** Spec compliance before code quality
4. ✅ **4-Phase Debugging Saves Time:** Systematic approach beats trial-and-error
5. ⚠️ **Full Installation Not Necessary:** Cherry-pick concepts into existing `.claude/skills/`

### Philosophy Alignment

Superpowers philosophy **aligns well** with Kite Class values:
- ✅ **Complexity Reduction** = Keep code simple (matches our principle)
- ✅ **Test-Driven Development** = We already encourage TDD
- ✅ **Systematic Processes** = Matches our git hooks & code quality checks
- ✅ **Evidence-Based** = Verify before completion (matches our testing policy)

### Actionable Recommendations

1. **Short-term (This Quarter):**
   - ✅ Add `systematic-debugging.md` skill với 4-phase process
   - ✅ Enhance implementation-plan.md với 2-5 min task breakdown
   - ✅ Update code-review.md với 2-stage review process

2. **Medium-term (Next Quarter):**
   - ✅ Implement TDD pre-commit hook (enforce RED-GREEN-REFACTOR)
   - ✅ Explore git worktrees cho parallel PRs
   - ✅ Document subagent usage patterns

3. **Long-term (Consider):**
   - ⚠️ Evaluate full Superpowers installation sau khi team familiar với concepts
   - ⚠️ Contribute back lessons learned to Superpowers community
   - ⚠️ Build custom skills specific to education domain

---

## 📝 Conclusion

**Superpowers** là một framework ấn tượng với **80.5k stars** và **strong community adoption**. Nó mang lại **systematic approach** cho AI-assisted development thông qua **mandatory workflows**, **TDD enforcement**, và **subagent coordination**.

### Overall Assessment

**Strengths:**
- ✅ Comprehensive workflow coverage (planning → debugging → review)
- ✅ Test-driven development enforcement
- ✅ Multi-platform support
- ✅ Strong community & active maintenance
- ✅ Open source (MIT) với clear methodology

**Weaknesses:**
- ⚠️ Shell-heavy implementation (54.8%)
- ⚠️ Platform dependency (requires compatible AI agent)
- ⚠️ Mandatory workflows có thể quá rigid
- ⚠️ Limited customization documentation

### Recommendation for Kite Class Platform

**ADOPT CONCEPTS, NOT FULL INSTALLATION** ⭐⭐⭐⭐☆ (4/5)

Thay vì install full framework, **cherry-pick valuable concepts** và integrate vào existing `.claude/skills/`:

1. ✅ Systematic debugging methodology
2. ✅ Two-stage code review process
3. ✅ Detailed task planning structure
4. ✅ TDD enforcement approach

Cách tiếp cận này cho phép Kite Class:
- Giữ flexibility của current workflow
- Tận dụng best practices từ Superpowers
- Tránh complexity của full installation
- Customize theo domain-specific needs (education platform)

---

**Report Generated:** 2026-03-13
**Author:** Claude Code
**Status:** ✅ Complete
**Next Review:** Q2 2026 (khi Superpowers release major update)
