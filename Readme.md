# RandomWalks-Demo

This project will perform random walks on a graph to determine the Valuable nodes as if they were from the original graph, using a similarity ranking measurement used by an attacker to exploit the system. 

## Youtube Demo Link :
-------


Before you begin, ensure you have met the following requirements:

- **Java:** The project typically requires Java. You can check if it's installed using:
  java -version

mark

- **SBT (Scala Build Tool):** This is essential to build and package Scala projects. If it's not already installed, follow the installation guidelines [here](https://www.scala-sbt.org/download.html).

## Building the Project using `sbt assembly`

1. **Clone the repository:**

git clone https://github.com/Abhishikth-Pammi/HomeWork-2.git
cd HomeWork-2

2. **Clean the project (Optional):**

It's generally a good practice to clean your project before assembling it.

sbt clean

3. **Compile the project:**

Ensure there are no compilation errors.

sbt compile

4**Run the assembly command:**

sbt assembly

Upon successful execution, this command creates a single assembly JAR file under the `target/scala-x.x.x/` directory. This JAR will contain your project's compiled class files as well as its dependencies.

5**Running the assembled JAR (Optional):**

If you want to run the application after assembling:

java -jar target/scala-2.13/HW2_Code-assembly-0.1.0-SNAPSHOT.jar

Run this Spark command
spark-submit --master local --class Main --jars HW2_Code-assembly-0.1.0-SNAPSHOT.jar --driver-class-path HW2_Code-assembly-0.1.0-SNAPSHOT.jar HW2_Code-assembly-0.1.0-SNAPSHOT.jar "classes/"

