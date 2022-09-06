package droidninja.filepicker.models

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.android.parcel.Parcelize


@Parcelize
class DirType constructor(
        var title: String,
        var type: Int,
        @DrawableRes
        var drawable: Int
) : Parcelable