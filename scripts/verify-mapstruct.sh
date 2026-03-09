#!/bin/bash
set -e

echo "🔍 MapStruct Verification Script"
echo "================================="
echo ""

# Check 1: AssignmentMapperImpl exists
echo "📋 Check 1: AssignmentMapperImpl.java exists..."
if [ -f "kiteclass/kiteclass-core/target/generated-sources/annotations/com/kiteclass/core/module/assignment/mapper/AssignmentMapperImpl.java" ]; then
    echo "✅ PASS: AssignmentMapperImpl.java found"
    GENERATED_DATE=$(grep "date = " kiteclass/kiteclass-core/target/generated-sources/annotations/com/kiteclass/core/module/assignment/mapper/AssignmentMapperImpl.java | head -1)
    echo "   $GENERATED_DATE"
else
    echo "❌ FAIL: AssignmentMapperImpl.java NOT found"
    exit 1
fi
echo ""

# Check 2: Compiled class exists
echo "📋 Check 2: AssignmentMapperImpl.class compiled..."
if [ -f "kiteclass/kiteclass-core/target/classes/com/kiteclass/core/module/assignment/mapper/AssignmentMapperImpl.class" ]; then
    echo "✅ PASS: AssignmentMapperImpl.class found"
else
    echo "❌ FAIL: AssignmentMapperImpl.class NOT found"
    exit 1
fi
echo ""

# Check 3: Maven compilation
echo "📋 Check 3: Maven compilation..."
cd kiteclass/kiteclass-core
ERROR_COUNT=$(./mvnw compile -DskipTests -q 2>&1 | grep -c -i "error" || true)
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo "✅ PASS: Maven compile successful (0 errors)"
else
    echo "❌ FAIL: Maven compile has $ERROR_COUNT errors"
    exit 1
fi
cd ../..
echo ""

# Check 4: No @Context parameter in mapper
echo "📋 Check 4: AssignmentMapper has no @Context parameter..."
CONTEXT_COUNT=$(grep -c "@Context" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/assignment/mapper/AssignmentMapper.java || true)
if [ "$CONTEXT_COUNT" -eq 0 ]; then
    echo "✅ PASS: No @Context parameter found (correct)"
else
    echo "❌ FAIL: Found @Context parameter ($CONTEXT_COUNT occurrences)"
    exit 1
fi
echo ""

# Summary
echo "================================="
echo "🎉 All checks passed!"
echo ""
echo "If IDE still shows error:"
echo "  1. Close IDE completely"
echo "  2. Delete .idea/ (IntelliJ) or .vscode/ (VS Code)"
echo "  3. Reopen project"
echo "  4. Wait for indexing to complete"
