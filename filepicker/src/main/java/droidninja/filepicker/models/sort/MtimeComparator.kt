package droidninja.filepicker.models.sort

import droidninja.filepicker.models.Document
import java.util.*

/**
 * Created by gabriel on 10/2/17.
 */
class MtimeComparator : Comparator<Document> {
    override fun compare(o1: Document, o2: Document): Int {
        var time1 = o1.mtime?.toUInt()
        var time2 = o2.mtime?.toUInt()
        if(time1 == null ) time1 = UInt.MIN_VALUE;
        if(time2 == null ) time2 = UInt.MIN_VALUE;
        return time1.compareTo(time2)
    }
}