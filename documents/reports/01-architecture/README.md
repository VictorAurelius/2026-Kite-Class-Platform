# System Architecture Documents

Comprehensive architecture documentation covering overall system design, component relationships, and infrastructure.

## 📑 Documents (6 files, 676KB)

### ⭐ Current Version
**system-architecture-v3-final.md** (286KB)
- Complete system architecture (latest)
- KiteHub: Modular Monolith (Sale, Message, Maintaining, AI Agent)
- KiteClass: Microservices (6-7 services per instance)
- AWS infrastructure (EKS, Aurora, S3, CloudFront)
- Multi-tenant hard isolation
- AI-powered branding automation

**Use this for**: Understanding overall system, new team member onboarding, architectural decisions

---

### Previous Versions
**system-architecture-v2-with-ai.md** (97KB)
- Version 2 with AI branding system added
- Keep for historical reference

**system-architecture-report.md** (83KB)
- Original architecture report (v1)
- Keep for historical reference

---

### Specialized Topics

**architecture-optimization-analysis.md** (89KB)
- Performance optimization strategies
- Bottleneck analysis
- Scaling recommendations
- Cost optimization

**gateway-core-separation-rationale.md** (26KB)
- Why Gateway and Core are separate services
- Security boundary explanation
- Multi-tenant isolation benefits
- Service responsibility breakdown

**kiteclass-node-provisioning.md** (69KB)
- Automated provisioning workflow (20 minutes total)
- AI branding generation (5 minutes)
- Infrastructure setup (15 minutes)
- Step-by-step technical details
- Kubernetes, database, service deployment

---

## 🎯 Quick Access

**Need to understand...**
- Overall architecture → `system-architecture-v3-final.md`
- Provisioning flow → `kiteclass-node-provisioning.md`
- Why Gateway/Core split → `gateway-core-separation-rationale.md`
- Performance improvements → `architecture-optimization-analysis.md`

**For thesis/presentation**
- Use `system-architecture-v3-final.md` (most comprehensive)
- Diagrams: See `../../diagrams/`

---

## 🔗 Related Documents
- Service details: `../02-service-analysis/service-use-cases-v3.md`
- Tech stack: `../03-technical-research/technology-stack-report.md`
- PlantUML diagrams: `../../diagrams/`
- Implementation plans: `../../plans/backend-implementation-plan-v2.md`
