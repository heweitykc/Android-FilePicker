package droidninja.filepicker.viewmodels

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import droidninja.filepicker.PickerManager
import droidninja.filepicker.models.Document
import droidninja.filepicker.models.FileType
import droidninja.filepicker.models.sort.StorageTypes
import droidninja.filepicker.utils.FilePickerUtils
import droidninja.filepicker.utils.StorageTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class VMDocPicker(application: Application) : BaseViewModel(application) {

    companion object {
        val APP_DIR_LIST = arrayOf(
            "/Android/data/com.tencent.mm/MicroMsg/Download/",
            "/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/"
        )
        var DOC_PROJECTION = arrayOf(MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.TITLE)

        var DOC_SELECTION_STR = ("${MediaStore.Files.FileColumns.MEDIA_TYPE}!=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                " AND ${MediaStore.Files.FileColumns.MEDIA_TYPE}!=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")
    }

    private val _lvDocData = MutableLiveData<HashMap<FileType, List<Document>>>()
    val lvDocData: LiveData<HashMap<FileType, List<Document>>>
        get() = _lvDocData


    fun getDocs(fileTypes: List<FileType>, storagetype: StorageTypes, comparator: Comparator<Document>?) {
        launchDataLoad {
            val dirs = queryDocs(fileTypes, storagetype, comparator)
            _lvDocData.postValue(dirs)
        }
    }

    @WorkerThread
    suspend fun queryDocs(fileTypes: List<FileType>, storagetype:StorageTypes, comparator: Comparator<Document>?): HashMap<FileType, List<Document>> {
        val documents = mutableListOf<Document>()

        withContext(Dispatchers.IO) {

            if (storagetype == StorageTypes.SPECIAL){
                for (exdir in APP_DIR_LIST){
                    val files = StorageTool.getFiles(getApplication<Application>().applicationContext, exdir)
                    if(files.isEmpty())  continue;
                    documents.addAll(getDocumentFromFiles(files))
                }
                return@withContext
            }

            val cursor = getApplication<Application>().contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                DOC_PROJECTION,
                DOC_SELECTION_STR,
                null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
            )
            if (cursor != null) {
                documents.addAll(getDocumentFromCursor(cursor))
                cursor.close()
            }

            if(storagetype == StorageTypes.ALL){
                for (exdir in APP_DIR_LIST){
                    val files = StorageTool.getFiles(getApplication<Application>().applicationContext, exdir)
                    if(files.isEmpty())  continue;
                    documents.addAll(getDocumentFromFiles(files))
                }
            }
        }
        return createDocumentType(fileTypes, comparator, documents)
    }

    @WorkerThread
    private fun createDocumentType(fileTypes: List<FileType>, comparator: Comparator<Document>?, documents: MutableList<Document>): HashMap<FileType, List<Document>> {
        val documentMap = HashMap<FileType, List<Document>>()

        for (fileType in fileTypes) {
            val documentListFilteredByType = documents.filter { document -> FilePickerUtils.contains(fileType.extensions, document.mimeType) }

            if(comparator != null){
                documentMap[fileType] = documentListFilteredByType.sortedWith(comparator)
            } else {
                documentMap[fileType] = documentListFilteredByType
            }
        }

        for ((key, documents) in documentMap) {
            for (fileitem in documents) {
                Log.e("FilePicker", fileitem.name);
//                Log.e("FilePicker", fileitem.path.path.toString());
                Log.e("FilePicker", fileitem.mtime.toString());
                Log.e("FilePicker", fileitem.size.toString());
//                Log.e("FilePicker", fileitem.mimeType.toString());
            }
        }

        return documentMap
    }

    @WorkerThread
    private fun getDocumentFromCursor(data: Cursor): MutableList<Document> {
        val documents = mutableListOf<Document>()
        while (data.moveToNext()) {

            val imageId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID))
            val path = data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            val title = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE))

            if (path != null) {

                val fileType = getFileType(PickerManager.getFileTypes(), path)
                val file = File(path)
                val contentUri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        imageId
                )
                if (fileType != null && !file.isDirectory && file.exists()) {

                    val document = Document(imageId, title, contentUri)
                    document.fileType = fileType

                    val mimeType = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                    if (mimeType != null && !TextUtils.isEmpty(mimeType)) {
                        document.mimeType = mimeType
                    } else {
                        document.mimeType = ""
                    }

                    document.size = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
                    document.mtime = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED))
                    if (!documents.contains(document)) documents.add(document)
                }
            }
        }

        return documents
    }

    @WorkerThread
    private fun getDocumentFromFiles(data: Array<DocumentFile>): MutableList<Document> {
        val documents = mutableListOf<Document>()
        for (fileitem in data){
            val imageId:Long = 0
            val path = fileitem.uri.path
            val title = fileitem.name
            val lastmodified = fileitem.lastModified()

            if (path != null && title != null) {
                val fileType = getFileType(PickerManager.getFileTypes(), path)
                val fileinfo = File(path)
                val document = title?.let { Document(imageId, it, fileitem.uri) }
                document.fileType = fileType
                document.size = fileinfo.length().toString()
                document.mtime = (lastmodified / 1000).toString()

                document.mimeType = StorageTool.getMimeType(fileinfo)
                if (!documents.contains(document)) documents.add(document)
            }
        }
        return documents
    }

    private fun getFileType(types: ArrayList<FileType>, path: String): FileType? {
        for (index in types.indices) {
            for (string in types[index].extensions) {
                if (path.endsWith(string)) return types[index]
            }
        }
        return null
    }
}