import NetGraphAlgebraDefs.NodeObject
import com.google.common.graph.EndpointPair

import scala.jdk.CollectionConverters._
import java.io.File
import java.util
import org.apache.spark._
import org.apache.spark.graphx.{Edge, EdgeDirection, Graph, VertexId}
import org.apache.spark.rdd.RDD

import scala.util.Random
import java.io.Serializable
import java.util.stream.Collectors
import scala.collection.mutable



object Main {

  def create_graph(sc: SparkContext,nodeListItem: java.util.Set[NetGraphAlgebraDefs.NodeObject], edgeListItem:java.util.Set[EndpointPair[NetGraphAlgebraDefs.NodeObject]]): Graph[NodeObject, Unit] = {
    // Sample list of NodeObjects
    val verticesList: List[NodeObject] = nodeListItem.asScala.toList;

    val vertices: RDD[(VertexId, NodeObject)] = sc.parallelize(
      verticesList.map(node => (node.id.toLong, node))
    )

    // Assuming you have a list of edges in the form of pairs of NodeObjects
    val edgePairs: List[(NodeObject, NodeObject)] = edgeListItem.asScala.toList.map(ep => (ep.nodeU(), ep.nodeV()))

    // Convert NodeObject pairs to Edge instances by referencing the unique IDs
    val edges: RDD[Edge[Unit]] = sc.parallelize(
      edgePairs.map { case (src, dst) =>
        Edge(src.id.toLong, dst.id.toLong, ()) // EdgeProperty is a placeholder for your edge property type
      }
    )

    // Convert list to RDD
    val verticesRDD: RDD[(VertexId, NodeObject)] = sc.parallelize(verticesList.map(node => (node.id.toLong, node)))

    // Assuming you have a list of edges as tuples (you will need to map from EndpointPair to Tuple)
    val edgesList: List[(Int, Int)] = List(
      (1, 264) // Replace with your actual edges represented as tuples (sourceId, destId)
    )

    // Convert list to RDD
    val edgesRDD: RDD[Edge[Unit]] = sc.parallelize(edgesList.map { case (srcId, dstId) =>
      Edge(srcId, dstId, ())
    })

    val graph: Graph[NodeObject, Unit] = Graph(vertices, edges)

    return graph

  }

  // Euclidean distance function
  def euclideanDistance(node1: NodeObject, node2: NodeObject): Double = {
    val attributes1 = Seq(
      node1.id.toDouble,
      node1.children.toDouble,
      node1.props.toDouble,
      node1.currentDepth.toDouble,
      node1.propValueRange.toDouble,
      node1.maxDepth.toDouble,
      node1.maxBranchingFactor.toDouble,
      node1.maxProperties.toDouble,
      node1.storedValue
    )
    val attributes2 = Seq(
      node2.id.toDouble,
      node2.children.toDouble,
      node2.props.toDouble,
      node2.currentDepth.toDouble,
      node2.propValueRange.toDouble,
      node2.maxDepth.toDouble,
      node2.maxBranchingFactor.toDouble,
      node2.maxProperties.toDouble,
      node2.storedValue
    )

    attributes1.zip(attributes2).map {
      case (x, y) => Math.pow(x - y, 2)
    }.sum
  }

  // Helper function to find the most similar node in g1 for a given node in g2
  def findMostSimilarNode(
                           node: NodeObject,
                           g1: Graph[NodeObject, Unit]
                         ): (VertexId, Double) = {
    g1.vertices.map { case (vertexId, otherNode) =>
      (vertexId, euclideanDistance(node, otherNode))
    }.min()(Ordering.by((_: (VertexId, Double))._2)) // Use min to find the smallest distance
  }

  // Main function to perform a random walk on g2 and update true/false positive counters
  def randomWalkWithCompleteMetrics(
                                     g1: Graph[NodeObject, Unit],
                                     g2: Graph[NodeObject, Unit],
                                     maxSteps: Int,
                                     valuableNodeList: Set[Int]
                                   ): (Set[Int], Set[Int], Set[Int], Set[Int]) = {

    var truePositives = mutable.Set[Int]()
    var falsePositives = mutable.Set[Int]()
    var trueNegatives = mutable.Set[Int]()
    var falseNegatives = mutable.Set[Int]()

    // Get the vertices in a random order and iterate through them
    val randomOrdering = Random.shuffle(g2.vertices.collect().toList)

    randomOrdering.foreach { case (vertexId, _) =>
      var currentVertexId = vertexId
      var steps = 0
      var visitedVertices = mutable.Set[VertexId]()

      while (steps < maxSteps && visitedVertices.size < g2.numVertices) {
        val currentVertexAttr = g2.vertices.lookup(currentVertexId).head
        val (mostSimilarVertexId, _) = findMostSimilarNode(currentVertexAttr, g1)

        if (valuableNodeList.contains(mostSimilarVertexId.toInt)) {
          if (!truePositives.contains(mostSimilarVertexId.toInt)) {
            truePositives.add(mostSimilarVertexId.toInt)
          } else {
            falseNegatives.add(mostSimilarVertexId.toInt) // Previously identified as true, now missed
          }
        } else {
          if (!falsePositives.contains(mostSimilarVertexId.toInt)) {
            falsePositives.add(mostSimilarVertexId.toInt)
          } else {
            trueNegatives.add(mostSimilarVertexId.toInt) // Previously missed, now correctly identified as not valuable
          }
        }

        visitedVertices.add(currentVertexId)
        val neighbors = g2.collectNeighborIds(EdgeDirection.Out).lookup(currentVertexId).flatten.distinct
        val unvisitedNeighbors = neighbors.filterNot(visitedVertices.contains)

        if (unvisitedNeighbors.nonEmpty) {
          currentVertexId = unvisitedNeighbors(Random.nextInt(unvisitedNeighbors.length))
          steps += 1
        } else {
          steps = maxSteps
        }
      }
    }

    (truePositives.toSet, falsePositives.toSet, trueNegatives.toSet, falseNegatives.toSet)
  }

  // Function to calculate metrics
  def calculateMetrics(
                        truePositives: Set[Int],
                        falsePositives: Set[Int],
                        trueNegatives: Set[Int],
                        falseNegatives: Set[Int]
                      ): (Double, Double, Double) = {

    val tp = truePositives.size
    val fp = falsePositives.size
    val tn = trueNegatives.size
    val fn = falseNegatives.size

    val accuracy = if (tp + tn + fp + fn > 0) (tp + tn).toDouble / (tp + tn + fp + fn) else 0.0
    val precision = if (tp + fp > 0) tp.toDouble / (tp + fp) else 0.0
    val recall = if (tp + fn > 0) tp.toDouble / (tp + fn) else 0.0

    (accuracy, precision, recall)
  }


  def spark_job(nodeList: util.ArrayList[java.util.Set[NetGraphAlgebraDefs.NodeObject]], edgeList:util.ArrayList[java.util.Set[EndpointPair[NetGraphAlgebraDefs.NodeObject]]], valuableNodeList: Set[Int]): Unit = {
    val conf = new SparkConf().setAppName("RandomWalksApp").setMaster("local")
    val sc = new SparkContext(conf)

    val graphList = new util.ArrayList[Graph[NodeObject, Unit]]()

    graphList.add(create_graph(sc, nodeList.get(0), edgeList.get(0)))
    graphList.add(create_graph(sc, nodeList.get(1), edgeList.get(1)))

    // Your graph definitions
    val graph1: Graph[NodeObject, Unit] = graphList.get(0)
    val graph2: Graph[NodeObject, Unit] = graphList.get(1)

    // Execute the random walk function
    val maxSteps = 1000 // or another number based on your graph size or requirements
    // You would then call the function like this:
    val (truePositives, falsePositives, trueNegatives, falseNegatives) =
      randomWalkWithCompleteMetrics(graph1, graph2, 100, valuableNodeList)

    // Assuming you've already computed truePositives, falsePositives, trueNegatives, and falseNegatives
    val (accuracy, precision, recall) = calculateMetrics(truePositives, falsePositives, trueNegatives, falseNegatives)

    println(s"Accuracy: $accuracy")
    println(s"Precision: $precision")
    println(s"Recall: $recall")

    // Finally, stop the Spark context when you're done
    sc.stop()
  }

  def main(args: Array[String]): Unit = {
    // Provide the directory path

    val dir = args(0)

    // Define a common prefix for the filenames
    val commonPrefix = "filename"

    // Create an array of possible extensions
    val extensions = Array(".ngs", ".ngs.perturbed")

    // Iterate through possible filenames and try to load them
    val loadedNetGraphs = extensions.flatMap { extension =>
      val fileName = commonPrefix + extension
      val file = new File(dir + fileName)
      println(file.getAbsolutePath)
      if (file.exists()) {
        println("exists")
        Some(NetGraphAlgebraDefs.NetGraph.load(fileName, dir))
      } else {
        println("not exists")
        None
      }
    }

    val nodeList = new util.ArrayList[java.util.Set[NetGraphAlgebraDefs.NodeObject]]()
    val edgeList = new util.ArrayList[java.util.Set[EndpointPair[NetGraphAlgebraDefs.NodeObject]]]()
    if (loadedNetGraphs.isEmpty) {
      // No files were loaded
      println("No NetGraph files found.")
    } else {
      // Process the loaded NetGraphs
      loadedNetGraphs.foreach {
        case Some(netGraph) =>
          // Successfully loaded a NetGraph
          val nodes = netGraph.sm.nodes()
          val edges = netGraph.sm.edges()
          nodeList.add(nodes)
          edgeList.add(edges)
          println(s"NetGraph loaded successfully: ${nodes.size} nodes found.")
        // You can now work with the loaded NetGraph here

        case None =>
          // Failed to load a NetGraph
          println("Failed to load NetGraph.")
      }
    }

    val javaList: java.util.Set[NetGraphAlgebraDefs.NodeObject] = nodeList.get(0)
    val scalaList: List[NetGraphAlgebraDefs.NodeObject] = javaList.asScala.toList
    val valuableNodeList = scalaList.filter(_.valuableData).map(_.id).toSet
    val valuableNodeVertexIds: Set[VertexId] = valuableNodeList.map(_.toLong)

    spark_job(nodeList, edgeList, valuableNodeList)
  }
}