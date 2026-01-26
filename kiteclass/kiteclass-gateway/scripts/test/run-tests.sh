#!/bin/bash

echo "=========================================="
echo "KiteClass Gateway - Test Runner"
echo "=========================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed!"
    echo ""
    echo "Please run the setup script first:"
    echo "  ./setup-java.sh"
    exit 1
fi

echo "✅ Java version:"
java -version
echo ""

# Change to project directory
cd "$(dirname "$0")"

# Run different test types based on argument
case "$1" in
    "unit")
        echo "Running unit tests only..."
        ./mvnw test -Dtest=*Test
        ;;
    "integration")
        echo "Running integration tests only..."
        ./mvnw test -Dtest=*IT
        ;;
    "service")
        echo "Running UserService tests..."
        ./mvnw test -Dtest=UserServiceTest
        ;;
    "controller")
        echo "Running UserController tests..."
        ./mvnw test -Dtest=UserControllerTest
        ;;
    "repository")
        echo "Running UserRepository tests..."
        ./mvnw test -Dtest=UserRepositoryTest
        ;;
    "compile")
        echo "Compiling project..."
        ./mvnw clean compile
        ;;
    "verify")
        echo "Running full verification (compile + test + coverage)..."
        ./mvnw clean verify
        ;;
    "coverage")
        echo "Running tests with coverage report..."
        ./mvnw clean test jacoco:report
        echo ""
        echo "Coverage report: target/site/jacoco/index.html"
        ;;
    *)
        echo "Running all tests..."
        ./mvnw clean test
        ;;
esac

TEST_RESULT=$?

echo ""
echo "=========================================="
if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ Tests passed successfully!"
else
    echo "❌ Tests failed! Exit code: $TEST_RESULT"
fi
echo "=========================================="

exit $TEST_RESULT
