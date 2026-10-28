# GATK 4.0.12.0 HaplotypeCaller optimisations

This branch produces a faster build of the GATK `HaplotypeCaller` version that was used to create the UK Biobank 500k release whole-genome sequencing VCFs and their indices ([UKB Field 23370](https://biobank.ndph.ox.ac.uk/showcase/field.cgi?id=23370)).

If you are not tied to the exact 4.0.12.0 calling pipeline, a modern maintained alternative is Tom Fennell's high-performance germline calling fork: [tfenne/gatk — `high_performance_germline_calling`](https://github.com/tfenne/gatk/tree/high_performance_germline_calling).

## Changes

- **Replaced the Intel Genomics Kernel Library (GKL) with FGKL 0.2.0** ([fulcrumgenomics/fgkl](https://github.com/fulcrumgenomics/fgkl)), Fulcrum Genomics' maintained fork of GKL — based on [GATK PR #9441](https://github.com/broadinstitute/gatk/pull/9441).
- **Backported additional performance fixes to the `ReferenceConfidenceModel`** hotspots from upstream GATK: [PR #5616](https://github.com/broadinstitute/gatk/pull/5616), [PR #5469](https://github.com/broadinstitute/gatk/pull/5469) and [PR #5652](https://github.com/broadinstitute/gatk/pull/5652).
- **Upgraded the runtime from Java 8 to Java 17** (required by FGKL).
- **Upgraded Gradle to 8.14.6** (needed for Java 17 support).

## Benchmark

Initial benchmarks ran `HaplotypeCaller` on chromosome 22 on an AMD Ryzen 7 3700X, with `OMP_NUM_THREADS=1` to simulate single-core usage on a cluster. The single-thread constraint matters because Intel GKL is multithreaded, while FGKL runs single-threaded — pinning both to one thread keeps the comparison fair for single-core cluster jobs.

| Java version | JAR version (GATK 4.0.12.0-…) | Smith-Waterman | User time (s) | Percentage | Peak memory (Max RSS) |
| --- | --- | --- | --- | --- | --- |
| Java 8 | orignal\_java8 | JAVA | 2369.52 | 100.00% | ~6.49 GB |
| Java 17 | 20-g62b2bb6 | FASTEST\_AVAILABLE | 1057.1 | 44.61% | ~1.78 GB |
| GraalVM 25.4.4.1.1 | 20-g62b2bb6 | FASTEST\_AVAILABLE | 891.12 | 37.61% | ~1.70 GB |
| GraalVM 25.4.4.1.1 | 23-gfbcd18d | FASTEST\_AVAILABLE | 877.37 | 37.03% | 2.09 GB |
