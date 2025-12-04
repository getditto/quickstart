package live.ditto.quickstart.dittowrapper

import android.os.Parcel
import android.os.Parcelable

data class QueryResult(
    val resultJson: List<String>,
    val mutatedIds: List<String> = emptyList()
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        resultJson = parcel.createStringArrayList() ?: emptyList(),
        mutatedIds = parcel.createStringArrayList() ?: emptyList()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(resultJson)
        dest.writeStringList(mutatedIds)
    }

    companion object CREATOR : Parcelable.Creator<QueryResult> {
        override fun createFromParcel(parcel: Parcel): QueryResult {
            return QueryResult(parcel)
        }

        override fun newArray(size: Int): Array<QueryResult?> {
            return arrayOfNulls(size)
        }
    }
}
