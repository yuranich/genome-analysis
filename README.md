# Genomic K-mers computation Playground
Here I explore Apache Spark distributed computation capabilities depending on environment and execution configuration.

Computational example is based on inspiring 23rd chapter from the great **[Apache Hadoop: The Definitive Guide](https://www.oreilly.com/library/view/hadoop-the-definitive/9781491901687/)** book.
In this chapter it is suggested to compute K-mers for genomic sequence using emerging *[ADAM](https://github.com/bigdatagenomics/adam)* framework.

More information about K-mer and its applications <br />
https://en.wikipedia.org/wiki/K-mer

## Main logic
```java
// Loading exome alignments dataset using ADAM framework
AlignmentDataset alignments = jac.loadAlignments(loadExomePath("ERR047879_1.fastq"));

// Specify computation
RDD<Tuple2<String, Object>> kmers = alignments.countKmers(21);

// Trigger the actual computation and return the result.
kmers.toJavaRDD()
        .sortBy(Tuple2::_2, false, kmers.getNumPartitions()).map(v1 -> v1._1() + ": " + v1._2)
        .take(20);
```

Real word EXOME for computations is taken from here
https://www.internationalgenome.org/data-portal/sample/NA21144

FTP: ftp://ftp.sra.ebi.ac.uk/vol1/fastq/ERR047/ERR047879/ERR047879_1.fastq.gz


## Computation configurations and results
Env setup: Intel i7-10700F 8(16) cores 2900GH and 64gb of DDR4 2400GH

### Local test in Driver JVM
**JVM**: amazon corretto 1.8

Configuration:
```java
new SparkConf(false)
            .setAppName("testADAM" + ": " + "sparkName")
            .setMaster("local[8]")
            .set("spark.driver.memory", "32g")
            .set("spark.driver.port", "50030")
            .set("spark.ui.enabled", "false")
            .set("spark.driver.allowMultipleContexts", "true");
```

Result:
- Real memory used ~ 17gb
- Execution time ~ 34 min
- Limiting factor: CPU

### Standalone Spark with single worker
**JVM**: amazon corretto 1.8 / openjdk 11

Configuration:
```shell
./spark-submit \
        --class home.yuranich.genomeanalysis.RunComputation \
        --master spark://yuranich.localdomain:7077 \
        --deploy-mode client \
        --executor-memory 32G \
        --total-executor-cores 8 \
        file:///genome-analysis/standalone-job/target/standalone-job-0.0.1-SNAPSHOT.jar
```

Result:
- Execution time on `amazon corretto 1.8` ~ 32 min
- Execution time on `openjdk 11` ~ 29 min

It is nice to see that `openjdk 11` run is actually faster!

#### Spark with Hadoop 
**Setup**: hadoop 2.7.7 pseudo distributed, started HDFS only.

Loaded `ERR047879_1.fastq` to HDFS (~ 5gb file)

**JVM**: amazon corretto 1.8

Result:
- Execution time ~ 41 min

kinda expected

### Spark on a GPU!!! (rtx 3060ti)

### Small k8s cluster on a single physical machine

### Small k8s cluster on two physical machines


## TODOs
Implement HTTP submit using apache-livy
Setup small cluster with 2-3 computers
