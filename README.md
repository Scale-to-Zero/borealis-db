# BorealisDB

BorealisDB is what we all hoped Aurora Serverless would have been when it was first announced: A scale-to-zero SQL database in the cloud. Scaling delays made Aurora Serverless v1 impractical for this envisioned purpose, and then in Aurora Serverless v2 the idea was killed by requiring a minimum amount of compute capacity to be provisioned. BorealisDB finally delivers "true scale-to-zero" functionality by combining AWS Lambda, sqlite3, and AWS Elastic File System.

The repo implements a serverless, SQL RDBMS in the cloud with fast cold-starts and instantaneous scale-to-zero. It ships with a minimal JDBC Driver for Java SQL clients. SQL clients for other languages are not currently implemented.

## Deployment

The provided `Dockerfile` uses the AWS SAM Build Image to build and deploy the database engine to AWS. This process assumes you have setup `.aws/credentials` in your home directory (either manually or using `aws configure`).

Usage:
> docker build -t borealis-builder .
>
> docker run --rm -v ~/.aws:/root/.aws -it borealis-builder

Docker is only used at build time to execute `sam build` and `sam deploy`. The resulting image is not deployed to AWS. The `Dockerfile` would be suitable for deployment in a CI/CD pipeline, but this is not provided in the current codebase.

By default, this will create a stack named `borealis-demo`. You may change this by editing the Dockerfile or overriding the variable with `-e SAM_STACK_NAME=...` in the `docker run` command.

## Usage

Copy the `jdbc-driver` project into your Maven local repo, and add the dependency to your project:
```
<dependency>
    <groupId>db.borealis</groupId>
    <artifactId>jdbc-driver</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

In your Java code, register the driver and connect to the database:

```
        String URL = "jdbc:borealis:<profile>:<region>:<stack-name>";

        DriverManager.registerDriver(new BorealisDriver());
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            // ... normal Java JDBC code here ...
        }
```

## Database connection URL format

The format is `jdbc:borealis:<profile>:<region>:<stack-name>` where:

- `<profile>`: Is the AWS Credentials profile name from `.aws/credentials`
- `<region>`: Your AWS region, e.g. `us-east-1`
- `<stack-name>`: Set this to `borealis-demo` unless you have changed the `SAM_STACK_NAME` in `Dockerfile`

## Authentication, Authorization, and Overall Security

The Elastic Filesystem and the Lambda Function implementing the Relational Database Engine are hosted in a private subnet within a dedicated VPC in your AWS account. The VPC is only exposed to the Lambda Function. The Lambda function is not exposed to the outside at all, and is subject to standard AWS authentication and authorization rules. This is quite secure and may even be too secure for some use-cases.

The JDBC connection uses the specified AWS Credentials Profile to invoke the AWS Lambda Function. The chosen IAM role needs to have `InvokeLambda` permission. The JDBC client may run outside of AWS, as long as it has the properly configured profile with the secret credentials.

The granularity of authorization is the entire database corresponding to the deployed stack. If you need more granular authorization, you'll need to deploy multiple BorealisDB stacks. Deploying multiple stacks instead of one big central datastore is the best-practice in a scale-to-zero serverless architecture.

## Architecture

The database engine implementation is internally in Python encapsulated as an AWS Lambda function. The Lambda is mounted to an Elastic File System for persistent storage. The client code is in Java.

The `sam-app` folder contains source code and supporting files for the serverless database engine that you can deploy with the SAM CLI. It includes the following files and folders:

- src - Code for the database engine's Lambda function.
- tests - Unit and Integration tests for the db engine implementation.
- template.yaml - A template that defines the engine's AWS resources.

The `jdbc-driver` folder contains the JDBC client for connecting to the serverless DB Engine from Java.

## Why was this built on sqlite3, instead of a more capable RDBMS engine?

Latest versions of sqlite3 are sufficiently capable for many common use-cases.

I evaluated the possibility of using a more capable engine such as PostresQL, MySQL, Apache Derby, and H2DB.

Sqlite3 has two required features which all the other engines lack:

- File locking. All the other engines were designed with the assumption that they have the filesystem all to themselves. Sqlite3 on the other hand, is capable of working with files that are also open in other Sqlite3 instances. Borealis' architecture involves multiple Lambda invocations connecting to the same EFS concurrently (when under sufficient load). Attempting to do this with any of the other engines would result in immediate data corruption.
- Start-up and shut-down time. Sqlite3 was designed as an embedded database that springs into existence when the embedding application (in this case the AWS Lambda invocation) starts, and goes away when the application exits. Other engines were optimized to perform as a long-running process, resulting in much slower start-up and shut-down. Interrupting the shut-down of these long-lived processes often comes with severe consequences.

The first obstacle with using a different engine could be overcome by limiting concurrency to 1, but that would exacerbate the second problem.

## Future Roadmap

I would accept volunteers and funding to help with the following:

- Finish implementing the JDBC Driver (many things are missing)
- Write database drivers for other languages
- Rewrite the core Lambda Function in Rust to shave a few milliseconds off cold starts
- Implement Lambda Response Streaming to get past the 6GB response size limit
- Implement repeatable (automated) performance benchmarks and comparison report
- Promote the project. Efficient computing helps combat global warming!

## Cleanup

To delete the deployed resources after testing this out, use the AWS CLI. Assuming you used the default name for the stack name, you can run the following:

```bash
sam delete --stack-name borealis-demo
```

## Resources

See the [AWS SAM developer guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html) for an introduction to SAM specification, the SAM CLI, and serverless application concepts.
