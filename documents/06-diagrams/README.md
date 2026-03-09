# System Diagrams

PlantUML diagrams for KiteClass architecture visualization.

## Structure
- `plantuml/`: Source .puml files
- `rendered/`: Generated PNG images
- `tools/`: plantuml.jar renderer

## Rendering
```bash
cd plantuml
java -jar ../tools/plantuml.jar -tpng *.puml
mv *.png ../rendered/
```

## Diagram Series
- `01-*`: Architecture simple overview
- `02-*`: Business Flow Diagram actors
- `03-*`: Entity Relationship Diagram
- `04-*`: Architecture full view
- `05-*`: System overview v3
- `06-*`: Business flow v3

## Standalone Diagrams
- `architecture-diagram.puml`: General architecture
- `provisioning-flow.puml`: Auto-provisioning flow
