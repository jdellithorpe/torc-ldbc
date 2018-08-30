/* 
 * Copyright (C) 2018 Stanford University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ellitron.ldbcsnbimpls.interactive.torc.util;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.incr;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.decr;
import static org.apache.tinkerpop.gremlin.process.traversal.P.*;
import static org.apache.tinkerpop.gremlin.process.traversal.Operator.assign;
import static org.apache.tinkerpop.gremlin.process.traversal.Operator.mult;
import static org.apache.tinkerpop.gremlin.process.traversal.Operator.minus;
import static org.apache.tinkerpop.gremlin.process.traversal.Scope.local;
import static org.apache.tinkerpop.gremlin.structure.Column.*;

import net.ellitron.torc.*;
import net.ellitron.torc.util.UInt128;
import net.ellitron.torc.TorcGraphProviderOptimizationStrategy;

import net.ellitron.ldbcsnbimpls.interactive.torc.*;

import org.apache.commons.configuration.BaseConfiguration;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import org.docopt.Docopt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A scratch pad for playing around with Gremlin queries on LDBC SNB datasets.
 * Here you can play around with Germlin query construction in Java.
 *
 * @author Jonathan Ellithorpe (jde@cs.stanford.edu)
 */
public class QueryScratchPad {
  private static final String doc =
      "QueryScratchPad: A scratch pad for playing around with Gremlin queries\n"
      + "on LDBC SNB datasets.\n" 
      + "\n"
      + "Usage:\n"
      + "  QueryScratchPad [options] COORDLOC GRAPHNAME\n"
      + "  QueryScratchPad (-h | --help)\n"
      + "  QueryScratchPad --version\n"
      + "\n"
      + "Arguments:\n"
      + "  COORDLOC    RAMCloud coordinator locator string.\n"
      + "  GRAPHNAME   Name of the graph in RAMCloud to connect to.\n"
      + "\n"
      + "Options:\n"
      + "  --dpdkPort=<n>    DPDK port [default: -1].\n"
      + "  -h --help         Show this screen.\n"
      + "  --version         Show version.\n"
      + "\n";


  public static void main(String[] args) throws Exception {
    Map<String, Object> opts =
        new Docopt(doc).withVersion("QueryScratchPad 1.0").parse(args);

    System.out.println(opts);

    String coordLoc = (String) opts.get("COORDLOC");
    String graphName = (String) opts.get("GRAPHNAME");
    int dpdkPort = Integer.decode((String) opts.get("--dpdkPort"));

    BaseConfiguration config = new BaseConfiguration();
    config.setDelimiterParsingDisabled(true);
    
    config.setProperty(
        TorcGraph.CONFIG_COORD_LOCATOR,
        coordLoc);
    
    config.setProperty(
        TorcGraph.CONFIG_GRAPH_NAME,
        graphName);

    config.setProperty(
        TorcGraph.CONFIG_DPDK_PORT,
        dpdkPort);

    Graph graph = TorcGraph.open(config);

    GraphTraversalSource g = graph.traversal();

    long personId = 1690L;
    long startDate = 1291161600000L;
    long durationDays = 43L;
    long endDate = startDate + (durationDays * 24L * 60L * 60L * 1000L);
    int limit = 10;

    final UInt128 torcPersonId = 
        new UInt128(TorcEntity.PERSON.idSpace, personId);

    /**
    * Given a start Person, find Tags that are attached to Posts that were
    * created by that Person’s friends. Only include Tags that were attached to
    * friends’ Posts created within a given time interval, and that were never
    * attached to friends’ Posts created before this interval. Return top 10
    * Tags, and the count of Posts, which were created within the given time
    * interval, that this Tag was attached to. Sort results descending by Post
    * count, and then ascending by Tag name.[1]
    */

    g.V(torcPersonId).out("knows")
      .in("hasCreator")
      .as("posts")
      .filter(t -> {
                long date = Long.valueOf(t.get().value("creationDate"));
                return date < startDate;
              })
      .as("beforeTimePosts")
      .out("hasTag").aggregate("beforeTimeTags")
      .select("posts")
      .filter(t -> {
                long date = Long.valueOf(t.get().value("creationDate"));
                return date <= endDate && date >= startDate;
              })
      .as("timeIntervalPosts")
      .out("hasTag")
      .where(without("beforeTimeTags"))
      .aggregate("timeIntervalFilteredTags")
      .groupCount()
      .order(local)
        .by(values, decr)
      .limit(local, limit)
      .select(keys)
      .unfold()
      .aggregate("topTags")
      .select("timeIntervalFilteredTags")
      .group().by(select("timeIntervalPosts"))

      .out("hasTag")
      .where(select("post").filter(t -> {
                long date = Long.valueOf(t.get().value("creationDate"));
                return date <= endDate && date >= startDate;
              })

    long start = System.nanoTime();
    while (gt.hasNext()) {
      System.out.println(gt.next().toString());
    }
    long end = System.nanoTime();

    System.out.println(String.format("Query Time: %dms", (end-start)/1000000L));

    graph.close();
  }
}
