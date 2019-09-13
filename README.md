[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/pennbiobank/pennai/carnival-public/master/LICENSE)

# Carnival

*It's a party of information!*


**Carnival** is a data unification technology that aggregates and semantically enriches (encodes the meaning of) data from disparate sources into a unified property graph resource and provides tools to reason over and interact with that resource.  **Carnival** has a robust architecture for tracking the provenance of data and providing evidence chains for conclusions or reasoning made on that data.  

Inspired by Open Biological and Biomedical Ontology (OBO) Foundry ontologies, the **carnival-clinical** extension of the **Carnival** data model supports the execution of common investigatory tasks including harmonizing complex patient, specimen and healthcare information, patient cohort identification, case-control matching, and the production of data sets for scientific analysis.

## Quick Links

* [Github Pages Website](https://pennbiobank.github.io/carnival/)
* [Developer Guide](docs/developer-setup.md) - Installation and running instructions
* [carnival-core](app/carnival-core/README.md) - Developer details on the core carnival module
* [carnival-gremlin-dsl](app/carnival-gremlin-dsl/README.md) - Gremlin dsl patterns for the carnival schema
* [graph specification](app/carnival-core/doc/graph.md)


## Contents

1. [Overview](#overview)
1. [Getting Started](#getting-started)

<a name="overview"></a>
## Overview
Carnval uses objects called *vines* to connect to external data sources and *reapers* encode the domain knowledge specific to that data source.  Vines can connect to sources such as MySql or Oracle databases, RedCap projects, and CSV files.  Some vine features include:

* Parameterized SQL queries
* Utilities to compose iterative SQL from lists of identifiers and codes
* Caching of query results
* Incremental caching of long running query result data
* Monitor thread to estimate time to completion of long running queries
* Automatic re-establishment of dropped connections
* API layer for REDCap
* H2 database wrapper for CSV data

Carnival’s property graph database:

* Is inherently schema-less enabling the incorporation of new data without restructuring resident data
* Follows data instantiation patterns built for computational efficiency and inspired by OBO Foundry ontologies
* Has a query engine capable of executing queries of arbitrary complexity

<a name="getting-started"></a>
## Getting Started
See [developer setup](docs/developer-setup.md) for full documentation on how to set up a development environment.