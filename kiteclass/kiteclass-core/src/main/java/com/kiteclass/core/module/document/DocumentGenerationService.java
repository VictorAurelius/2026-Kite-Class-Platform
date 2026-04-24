package com.kiteclass.core.module.document;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Facade over format-specific {@link Generator} strategies (per ADR-019 and design-patterns.md §3).
 *
 * <p>Callers (invoice service, attendance service, etc.) depend only on this facade — they never
 * see PDFBox, POI, or XWPF types (enforces the "no leaky abstraction" rule from design-patterns.md
 * §3.10). Generators are discovered from the Spring context: wiring a new format in a later sub-PR
 * requires only a new {@code @Component} implementing {@link Generator}.
 *
 * <p>Sub-PR 5.0 ships this facade with an empty generator map — every call throws
 * {@link UnsupportedOperationException} until the corresponding sub-PR wires its generator.
 */
@Service
public class DocumentGenerationService {

    private final Map<DocumentFormat, Generator> generators;

    public DocumentGenerationService(List<Generator> generators) {
        this.generators = new EnumMap<>(DocumentFormat.class);
        for (Generator g : generators) {
            this.generators.put(g.format(), g);
        }
    }

    public DocumentResponse generate(DocumentRequest request) {
        Generator g = generators.get(request.format());
        if (g == null) {
            throw new UnsupportedOperationException(
                    request.format() + " generator not wired yet — see Wave 5 Sub-PRs 5.1–5.3");
        }
        return g.generate(request);
    }
}
