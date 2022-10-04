package org.fcitx.fcitx5.android.utils

import java.util.*

class ImmutableGraph<V, L>(
    edges: List<Edge<V, L>>
) {

    data class Edge<V, L>(val vertex1: V, val vertex2: V, val label: L)

    val vertices = edges
        .flatMap { listOf(it.vertex1, it.vertex2) }
        .distinct()

    val labels = edges
        .map { it.label }
        .distinct()

    val adjacencyMatrix = Array(vertices.size) {
        IntArray(vertices.size) { -1 }
    }.apply {
        edges.forEach { (v1, v2, l) ->
            this[vertices.indexOf(v1)][vertices.indexOf(v2)] = labels.indexOf(l)
        }
    }

    fun getEdgesOfVertexWithIndex(vertex: V) = adjacencyMatrix[vertices.indexOf(vertex)]
        .asIterable()
        .mapIndexedNotNull { v2Idx, labelIdx ->
            labelIdx.takeIf { it != -1 }?.run {
                (v2Idx to labelIdx) to Edge(vertex, vertices[v2Idx], labels[labelIdx])
            }
        }

    fun bfs(vertex: V): List<Pair<V, L>> {
        val start = vertices.indexOf(vertex).takeIf { it != -1 } ?: return emptyList()
        val visited = BooleanArray(vertices.size)
        val queue: Queue<Pair<Int, Int>> = LinkedList()
        val result = mutableListOf<Pair<Int, Int>>()
        visited[start] = true
        queue.add(start to -1)
        while (queue.isNotEmpty()) {
            val (x, v) = queue.remove()
            if (start != x)
                result.add(x to v)
            visited.indices.forEach { i ->
                val l = adjacencyMatrix[x][i].takeIf { it != -1 }
                if (l != null && !visited[i]) {
                    queue.add(i to l)
                    visited[i] = true
                }
            }
        }
        return result.map { (v, l) -> vertices[v] to labels[l] }
    }

}