#!/bin/bash
# IDE Problems Check Script
# Run this to check for Java compile errors, Checkstyle violations, and TypeScript errors

set -e

# Setup Java 21
export JAVA_HOME="/home/vkiet/.local/java/jdk-21.0.5+11"
export PATH="$JAVA_HOME/bin:$PATH"

# Setup pnpm
export PNPM_HOME="/home/vkiet/.local/share/pnpm"
export PATH="$PNPM_HOME:$PATH"

MVN="$HOME/.m2/wrapper/dists/apache-maven-3.9.6/bin/mvn"

echo "========================================="
echo "IDE Problems Check"
echo "========================================="
echo ""

# Backend - Core Service
echo "📦 Checking Core Service (Java + Checkstyle)..."
cd kiteclass/kiteclass-core
$MVN compile -q
echo "✅ Core Service: OK"
echo ""

# Backend - Gateway Service
echo "📦 Checking Gateway Service (Java + Checkstyle)..."
cd ../kiteclass-gateway
$MVN compile -q
echo "✅ Gateway Service: OK"
echo ""

# Frontend
echo "📦 Checking Frontend (TypeScript + ESLint)..."
cd ../kiteclass-frontend

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    pnpm install --frozen-lockfile
fi

# TypeScript check
echo "  → TypeScript type check..."
node_modules/.bin/tsc --noEmit

# ESLint
echo "  → ESLint check..."
node_modules/.bin/next lint

echo "✅ Frontend: OK"
echo ""
echo "========================================="
echo "✅ All checks passed! IDE should be clean."
echo "========================================="
