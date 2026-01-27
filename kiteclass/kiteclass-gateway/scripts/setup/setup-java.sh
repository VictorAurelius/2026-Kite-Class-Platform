#!/bin/bash

echo "=========================================="
echo "Installing Java 17 JDK in WSL"
echo "=========================================="

# Update package list
echo "Step 1: Updating package list..."
sudo apt update

# Install OpenJDK 17
echo ""
echo "Step 2: Installing OpenJDK 17 JDK..."
sudo apt install -y openjdk-17-jdk

# Set JAVA_HOME
echo ""
echo "Step 3: Setting up JAVA_HOME..."
JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
echo "JAVA_HOME will be: $JAVA_HOME_PATH"

# Add to ~/.bashrc if not already present
if ! grep -q "JAVA_HOME" ~/.bashrc; then
    echo "" >> ~/.bashrc
    echo "# Java Environment" >> ~/.bashrc
    echo "export JAVA_HOME=$JAVA_HOME_PATH" >> ~/.bashrc
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
    echo "Added JAVA_HOME to ~/.bashrc"
else
    echo "JAVA_HOME already exists in ~/.bashrc"
fi

# Export for current session
export JAVA_HOME=$JAVA_HOME_PATH
export PATH=$JAVA_HOME/bin:$PATH

# Verify installation
echo ""
echo "=========================================="
echo "Verification:"
echo "=========================================="
java -version
javac -version
echo ""
echo "JAVA_HOME: $JAVA_HOME"

echo ""
echo "=========================================="
echo "Installation complete!"
echo "=========================================="
echo ""
echo "To use Java in new terminal sessions, run:"
echo "  source ~/.bashrc"
echo ""
echo "To run tests now:"
echo "  cd $(pwd)"
echo "  ./mvnw test"
