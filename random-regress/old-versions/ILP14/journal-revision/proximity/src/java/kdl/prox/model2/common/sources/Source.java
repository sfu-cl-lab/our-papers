/**
 * $Id: Source.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.common.sources;

import kdl.prox.db.Container;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.NSTCreator;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.List;

/**
 * Represents a source for a model. It is used in the pre-processing pass, when the Feature Vectors are created,
 * to specify the source for the data. Example implementations are an attribute, a temporal attribute,
 * or a structural attribute
 * <p/>
 * How to create a new Source.
 * - Extend this class
 * - define toString()
 * - define computeSourceTable(), which should return a [subg_id, value] NST
 * <p/>
 * - By default, the type of the source is the type of the value column in sourceTable. Overload getType if you need to
 * - By default, the source is continuous if the type is FLT or DBL, discrete otherwise. Overload isContinuous if you need to
 * - By default, the number of bins is the class's default. Overload getNumDistinctContinuousValues if you need to
 * -   [All these options (type, continuous, bins) can be set manually via  standard setters]
 * <p/>
 * - by default, the distinct values are taken from the [value] column, binned if continuous. Overload computeDistinctValues if you have to
 * - by default, the distinct subgraphs are computed from the sourceTable. Overload computeDistinctSubgraphs if you have to (see ItemSource, eg)
 * - the values for distinctValues and distinctSubgraphs are cached. Overload the getters if for some reason this is not good for you
 */
public abstract class Source {

    static Logger log = Logger.getLogger(AttributeSource.class);

    protected int numDistinctContinuousValues = 2;

    protected DataTypeEnum type;
    protected Boolean isContinuous;
    protected Boolean isSingleValue;  // true if the source has a singe row per subgraph --in that case, there's no need to use aggregators!

    protected NST sourceTable;
    protected List distinctValues;
    protected NST distinctSubgraphs;
    private int rowCount;
    private int subgraphCount;


    public Source() {
    }

    /**
     * Create an instance of Source with the values specified in the XML element
     * Derived classes can extend this constructor to read in values from the source-elements child
     *
     * @param containerEle
     */
    public Source(Element containerEle) {
        Element sourceEle = containerEle.getChild("source");
        setType(DataTypeEnum.enumForType(sourceEle.getChildText("data-type")));
        setIsContinuous(Boolean.valueOf(sourceEle.getChildText("is-continuous")).booleanValue());
        setIsSingleValue(Boolean.valueOf(sourceEle.getChildText("is-single-value")).booleanValue());
    }


    protected void assertIsInitialized() {
        Assert.notNull(sourceTable, "You must initialize the source with a container first. Use init()");
    }

    /**
     * Find the different values in the source table
     *
     * @return a list of strings with the unique values from the source
     */
    protected List computeDistinctValues() {
        assertIsInitialized();
        if (isContinuous()) {
            List distinctVals = sourceTable.getDistinctColumnValuesBinned("value", getNumDistinctContinuousValues() + 1);
            if (distinctVals.size() <= numDistinctContinuousValues) {
                return distinctVals;
            } else {
                return distinctVals.subList(0, numDistinctContinuousValues);
            }
        } else {
            return sourceTable.getDistinctColumnValues("value");
        }
    }

    /**
     * Find the distinct subgraphs [subg_id]
     *
     * @return an NST with distinct subgraphs [subg_id, count]
     */
    protected NST computeDistinctSubgraphs() {
        assertIsInitialized();
        return sourceTable.getColumnHistogramAsNST("subg_id");
    }

    public abstract boolean equals(Object x);

    /**
     * This is the main method. It creates sourceTable, the [subg_id, value] NSTs that represent the source.
     *
     * @return an NST of type [subg_id, value]
     */
    protected abstract NST computeSourceTable(Container cont, NSTCache cache);


    public int getNumDistinctContinuousValues() {
        return numDistinctContinuousValues;
    }

    /**
     * Returns the List of distinct values in the source, which was computed during init()
     *
     * @return List of distinct values in the source, which was computed during init()
     */
    public List getDistinctValues() {
        assertIsInitialized();
        return distinctValues;
    }

    /**
     * Returns the NST with [subg_id] of distinct subg_id, which was computed during init()
     *
     * @return NST with [subg_id] of distinct subg_id
     */
    public NST getDistinctSubgraphs() {
        assertIsInitialized();
        return distinctSubgraphs;
    }

    /**
     * Returns the NST with [subg_id, value], which was computed during init()
     *
     * @return NST with [subg_id, value]
     */
    public NST getSourceTable() {
        assertIsInitialized();
        return sourceTable;
    }

    /**
     * Get source type
     *
     * @return a DataTypeEnum for the type of this attr
     */
    public DataTypeEnum getType() {
        Assert.notNull(type, "type hasn't been set yet");
        return type;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Passes in the container, and initializes all the tables it needs
     *
     * @param cont
     * @param cache
     */
    public Source init(final Container cont, final NSTCache cache) {
        sourceTable = cache.getOrCreateTable("src" + this.toString(), new NSTCreator() {
            public NST create() {
                return computeSourceTable(cont, cache);
            }
        });
        rowCount = sourceTable.getRowCount();
        if (rowCount == 0) {
            log.warn("Source table is empty: " + this.toString());
        }

        distinctSubgraphs = cache.getOrCreateTable("src" + this.toString() + "_subgs", new NSTCreator() {
            public NST create() {
                return computeDistinctSubgraphs();
            }
        });
        subgraphCount = distinctSubgraphs.getRowCount();

        // get the types, etc.
        type = sourceTable.getNSTColumn("value").getType();
        if (isContinuous == null) {
            isContinuous = Boolean.valueOf(getType() == DataTypeEnum.FLT || getType() == DataTypeEnum.DBL);
        }
        if (isSingleValue == null) {
            isSingleValue = Boolean.valueOf(rowCount == subgraphCount);
        }

        // get the distinct values
        distinctValues = computeDistinctValues();

        return this;
    }

    public boolean isContinuous() {
        Assert.notNull(isContinuous, "isContinuous hasn't been set yet");
        return isContinuous.booleanValue();
    }

    /**
     * Is this source single value?
     *
     * @return true if there is a single row per subgraph
     */
    public boolean isSingleValue() {
        Assert.notNull(isSingleValue, "isSingleValue hasn't been set yet");
        return isSingleValue.booleanValue();
    }

    public Source setIsContinuous(boolean isContinuous) {
        this.isContinuous = Boolean.valueOf(isContinuous);
        return this;
    }

    public Source setIsSingleValue(boolean isSingleValue) {
        this.isSingleValue = Boolean.valueOf(isSingleValue);
        return this;
    }

    public void setNumDistinctContinuousValues(int numDistinctContinuousValues) {
        this.numDistinctContinuousValues = numDistinctContinuousValues;
    }

    public void setType(DataTypeEnum type) {
        this.type = type;
    }

    public abstract String toString();

    /**
     * Creates an XML element to represent the source
     * Creates the basic elements, and then derived classes can extend this and add elements to the source-elements child
     *
     * @return an XML element with a representation of the source
     */
    public Element toXML() {
        Element sourceTop = new Element("source");
        sourceTop.addContent(XMLUtil.createElementWithValue("source-class", this.getClass().getName()));
        sourceTop.addContent(XMLUtil.createElementWithValue("data-type", type.toString()));
        sourceTop.addContent(XMLUtil.createElementWithValue("is-continuous", isContinuous.toString()));
        sourceTop.addContent(XMLUtil.createElementWithValue("is-single-value", isSingleValue.toString()));
        sourceTop.addContent(new Element("source-elements"));
        return sourceTop;
    }
}
