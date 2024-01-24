package dev.samuelab.immigrationservice.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tho Mar 19, 2015 10:43:22 PM
 */
public class TrapRangeBuilder {
    private final Logger logger = LoggerFactory.getLogger(TrapRangeBuilder.class);
    private final List<Range<Integer>> ranges = new ArrayList<>();


    public TrapRangeBuilder addRange(Range<Integer> range) {
        ranges.add(range);
        return this;
    }



    /**
     * The result will be ordered by lowerEndpoint ASC
     *
     * @return
     */
    public List<Range<Integer>> build() {
        List<Range<Integer>> retVal = new ArrayList<>();
        List<Range<Integer>> newRetVal = new ArrayList<>();

        // order range by lower Bound
        Collections.sort(ranges, new Comparator<Range>() {
            @Override
            public int compare(Range o1, Range o2) {
                return o1.lowerEndpoint().compareTo(o2.lowerEndpoint());
            }
        });



        for (int i = 0; i < ranges.size(); i++) {
            // System.out.println(textsCol);

            if (retVal.isEmpty()) {
                retVal.add(ranges.get(i));
            } else {

                Range<Integer> lastRange = retVal.get(retVal.size() - 1);

                if (lastRange.isConnected(ranges.get(i))) {
                    Range newLastRange = lastRange.span(ranges.get(i));
                    retVal.set(retVal.size() - 1, newLastRange);
                }
                else {

                    retVal.add(ranges.get(i));
                }
                // }

            }
        }
        newRetVal.addAll(retVal);


        // debug
        logger.debug("Found " + retVal.size() + " trap-range(s)");

        return retVal;
    }
}