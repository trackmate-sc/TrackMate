# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TrackMate is a Fiji plugin for single particle tracking in microscopy images. It provides a modular, extensible framework for detecting spots (particles) in images and linking them across time frames to create tracks. The plugin is designed for both end-users (through a wizard-based GUI) and developers (through extensive extension points).

**Key capabilities:**
- Spot detection in 2D/3D+Time images
- Particle linking (tracking) with multiple algorithms
- Feature computation for spots, tracks, and edges
- Visualization and analysis tools
- Export to various formats
- Integration with Python tools via Appose

## Build and Test Commands

**Build the project:**
```bash
mvn clean install
```

**Run tests:**
```bash
mvn test
```

**Run a single test:**
```bash
mvn test -Dtest=ClassName
# Example: mvn test -Dtest=ModelTest
```

**Skip tests during build:**
```bash
mvn clean install -DskipTests
```

**Check code style:**
The project follows ImgLib2 coding style (see scijava-coding-style). No separate lint command configured.

## Requirements

- Java 21+ (TrackMate v9 requires Java 21)
- Maven for building
- Uses Maven parent POM: `org.scijava:pom-scijava:43.0.0`

## Architecture Overview

### Core Classes

**TrackMate** (`fiji.plugin.trackmate.TrackMate`)
- Main orchestrator class that executes the tracking pipeline
- Coordinates detection → filtering → tracking → feature computation
- Takes `Settings` and populates a `Model`

**Model** (`fiji.plugin.trackmate.Model`)
- Central data structure holding all tracking results
- Contains `SpotCollection`, `TrackModel`, and `FeatureModel`
- Implements transaction model for batch updates
- Fires `ModelChangeEvent` to listeners

**Settings** (`fiji.plugin.trackmate.Settings`)
- Configuration container for the tracking process
- Holds detector/tracker factories and their settings
- Stores image metadata and ROI information

**SpotCollection** (`fiji.plugin.trackmate.SpotCollection`)
- Time-indexed collection of detected spots
- Thread-safe for concurrent detection

**TrackModel** (`fiji.plugin.trackmate.TrackModel`)
- Graph structure representing tracks
- Built on JGraphT (`SimpleWeightedGraph`)
- Edges connect spots across frames

### Extensibility System

TrackMate uses SciJava plugin discovery for all extension points. To extend TrackMate, create a class implementing the appropriate interface and annotate with `@Plugin`.

**Seven extension types:**

1. **SpotDetectorFactory** (`fiji.plugin.trackmate.detection.SpotDetectorFactoryBase`)
   - Two variants: `SpotDetectorFactory` (per-frame) and `SpotGlobalDetectorFactory` (all frames)
   - Examples: `DogDetectorFactory`, `LogDetectorFactory`, `ThresholdDetectorFactory`

2. **SpotAnalyzerFactory** (`fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory`)
   - Computes numerical features for spots (intensity, shape, etc.)
   - Runs after detection

3. **EdgeAnalyzer** (`fiji.plugin.trackmate.features.edges.EdgeAnalyzer`)
   - Computes features for links between spots (velocity, displacement)

4. **TrackAnalyzer** (`fiji.plugin.trackmate.features.track.TrackAnalyzer`)
   - Computes features for entire tracks (total displacement, mean speed)

5. **SpotTrackerFactory** (`fiji.plugin.trackmate.tracking.SpotTrackerFactory`)
   - Creates trackers that link spots into tracks
   - Examples: `jaqaman/`, `kalman/`, `overlap/`

6. **ViewFactory** (`fiji.plugin.trackmate.visualization.ViewFactory`)
   - Creates visualizations of tracking results

7. **TrackMateActionFactory** (`fiji.plugin.trackmate.action.TrackMateActionFactory`)
   - Post-processing actions (export, analysis, visualization)

**Plugin annotation example:**
```java
@Plugin(type = SpotDetectorFactory.class, priority = Priority.NORMAL, visible = true)
public class MyDetectorFactory implements SpotDetectorFactory<FloatType> {
    // implementation
}
```

### Provider System

Providers (`fiji.plugin.trackmate.providers/`) use SciJava plugin discovery to collect all implementations:
- `DetectorProvider` - finds all detector factories
- `TrackerProvider` - finds all tracker factories
- `SpotAnalyzerProvider` - finds all spot analyzers
- `ActionProvider` - finds all actions
- etc.

### Package Structure

```
fiji.plugin.trackmate/
├── detection/          - Spot detector implementations
│   ├── semiauto/      - Semi-automatic detection tools
│   └── util/          - Detection utilities
├── tracking/          - Particle linking algorithms
│   ├── jaqaman/       - LAP tracker (Jaqaman et al.)
│   ├── kalman/        - Kalman filter tracker
│   ├── kdtree/        - Nearest neighbor tracker
│   └── overlap/       - Overlap tracker
├── features/          - Feature computation
│   ├── spot/          - Spot features
│   ├── edges/         - Edge features
│   └── track/         - Track features
├── visualization/     - Views and rendering
├── gui/               - User interface components
│   ├── wizard/        - Main wizard interface
│   └── components/    - Reusable GUI components
├── action/            - Post-processing actions
├── io/                - XML serialization and import/export
├── providers/         - Plugin discovery providers
├── graph/             - Graph data structures
└── util/              - Utilities and helpers
    └── cli/           - Command-line interface tools
        └── appose/    - Appose integration for Python
```

## Key Concepts

### Detection Flow

1. **Per-frame detectors** (`SpotDetectorFactory`): Process one time-point at a time, parallelized by TrackMate
2. **Global detectors** (`SpotGlobalDetectorFactory`): Process entire image stack at once, handle own parallelization

### Tracking Flow

1. Detection produces `SpotCollection`
2. Tracker receives `SpotCollection` and creates links
3. Links form directed graph in `TrackModel`
4. Features computed on spots, edges, and tracks

### Appose Integration (v9+)

TrackMate now integrates with Python tools via Appose:
- `ApposeConfigurator` (`fiji.plugin.trackmate.util.cli.appose/`) - manages Python environments
- `ApposeDetector` - generic detector using Python scripts
- Uses shared memory (`imglib2-appose`) for efficient data transfer
- Example: `CellposeAppose` integrates Cellpose segmentation

**Configurator pattern:**
- `Configurator` interface for external tool configuration
- `CLIConfigurator` for command-line tools
- `ApposeConfigurator` for Python via Appose

## Data Model

**Spot representation:**
- Base type: `SpotBase` (immutable position/properties)
- `Spot` - classic spot with radius
- `SpotRoi` - spot with ImageJ ROI contour
- `SpotMesh` - spot with 3D mesh

**Features:**
- Stored in `FeatureModel` as Map<Spot/Edge/Track, Map<String, Double>>
- Feature keys defined in analyzer classes
- Features filterable via `FeatureFilter`

## Testing

Tests in `src/test/java/fiji/plugin/trackmate/`:
- Unit tests: `ModelTest`, `SpotCollectionTest`, `TrackModelTest`
- Example code: Look for `*Example.java` files demonstrating API usage
- Interactive tests in `interactivetests/` (require manual verification)

**Example test locations:**
- `tracking/sparselap/SparseLAPTrackerExample.java` - tracker usage
- `appose/ApposePlayground.java` - Appose/Python integration
- `visualization/table/TrackMateTableExample.java` - data export

## XML Serialization

Files use `.xml` extension with TrackMate schema:
- `TmXmlReader` / `TmXmlWriter` in `fiji.plugin.trackmate.io`
- `TmXmlKeys` defines all XML element/attribute keys
- `SettingsPersistence` handles settings serialization

## Development Notes

- **Thread safety**: Detection and feature computation use multi-threading. Detectors implement `MultiThreaded` interface.
- **Transaction model**: When modifying `Model`, use `beginUpdate()` / `endUpdate()` to batch changes and fire single event.
- **Logger**: Use `fiji.plugin.trackmate.Logger` interface for progress reporting (not java.util.logging).
- **Cancelable**: Long-running operations implement `org.scijava.Cancelable` for user cancellation.
- **ImgLib2**: Core image processing uses ImgLib2 (`net.imglib2.*`), not ImageJ's ImageProcessor directly.

## Citation

If testing with real data, note that TrackMate is a published tool:
Jean-Yves Tinevez et al., "TrackMate: An open and extensible platform for single-particle tracking", Methods, 2016. http://dx.doi.org/10.1016/j.ymeth.2016.09.016
