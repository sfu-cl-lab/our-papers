/**
 * $Id: RptRenderer.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;


import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.visualization.ArrowFactory;
import edu.uci.ics.jung.visualization.PluggableRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * We define our own type of renderer for the RPT Viewer, with its own
 * functions for drawing vertex icons, writing text, changing the thickness of the edges, etc.
 */
public class RptRenderer extends PluggableRenderer {

    public RptRenderer() {
        super();

        //Sets the nodes to be drawn with RptVertexIcon
        this.setVertexIconFunction(new DefaultVertexIconFunction() {
            public Icon getIcon(ArchetypeVertex v) {
                RptVertex dsv = (RptVertex) v;
                return new RptVertexIcon(dsv);
            }
        });

        this.setVertexStringer(new VertexStringer() {
            public String getLabel(ArchetypeVertex v) {
                return ((RptVertex) v).vertexString();
            }
        });

        this.setEdgeStringer(new EdgeStringer() {
            public String getLabel(ArchetypeEdge e) {
                return null;
            }
        });

        //Sets how the thickness of edges is drawn
        // fullWeight is the max thickness a line can be
        // if it was desired to add a min you would just add
        // weight = Max(weight, minThickness)
        this.setEdgeStrokeFunction(new EdgeStrokeFunction() {
            public Stroke getStroke(Edge e) {
                int fullWeight = 20;
                double weight;
                weight = ((RptDirectedEdge) e).getWeight();
                weight *= fullWeight;
                weight = Math.min(fullWeight, weight);
                return new BasicStroke((int) weight);
            }
        });

        //Sets edges to be straight lines
        this.setEdgeShapeFunction(new EdgeShape.Line());

        //Directed edges automatically have a wedged arrow
        //so we set the wedge to be one of size 0,0
        this.setEdgeArrowFunction(new EdgeArrowFunction() {
            public Shape getArrow(Edge e) {
                return ArrowFactory.getWedgeArrow(0, 0);
            }
        });

    }

    /**
     * Labels the specified vertex with the specified label.
     * Uses the font specified by this instance's
     * <code>VertexFontFunction</code>.  (If the font is unspecified, the existing
     * font for the graphics context is used.)  If vertex label centering
     * is active, the label is centered on the position of the vertex; otherwise
     * the label is offset slightly.
     */
    protected void labelVertex(Graphics g, Vertex v, String label, int x, int y) {
        Component component = prepareRenderer(graphLabelRenderer, label, isPicked(v), v);

        Dimension d = component.getPreferredSize();
        Dimension i = ((RptVertex) v).getIconSize();

        int h_offset;
        int v_offset;

        h_offset = -d.width / 2;
        v_offset = -i.height / 2;

        if (((RptVertex) v).isLeaf())
//            v_offset = ((RptVertex) v).getBarThickness() - 80;
            v_offset = -5; //just a small spacer to keep text within the box

        rendererPane.paintComponent(g, component, screenDevice, x + h_offset, y + v_offset,
                i.width, i.height - ((RptVertex) v).getBarThickness(), true);

    }

}
