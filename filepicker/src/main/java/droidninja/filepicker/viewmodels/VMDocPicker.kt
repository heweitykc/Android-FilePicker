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
    val WEIXIN_DIR = "/Android/data/com.tencent.mm/MicroMsg/Download/"
    val QQ_DIR = "/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/"

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
        var data = HashMap<FileType, List<Document>>()


        if(storagetype == StorageTypes.COMMON){
            withContext(Dispatchers.IO) {
                val selection = ("${MediaStore.Files.FileColumns.MEDIA_TYPE}!=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                        " AND ${MediaStore.Files.FileColumns.MEDIA_TYPE}!=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")

                val DOC_PROJECTION = arrayOf(MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.TITLE)

                val cursor = getApplication<Application>().contentResolver.query(MediaStore.Files.getContentUri("external"), DOC_PROJECTION, selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC")

                if (cursor != null) {
                    data = createDocumentType(fileTypes, comparator, getDocumentFromCursor(cursor))
                    cursor.close()
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                var exdir = ""
                if(storagetype == StorageTypes.QQ){
                    exdir = QQ_DIR
                } else if(storagetype == StorageTypes.WEIXIN){
                    exdir = WEIXIN_DIR
                }
                if(exdir.isNotEmpty()){
                    val files = StorageTool.getFiles(getApplication<Application>().applicationContext, exdir)
                    if(files.isNotEmpty()){
                        data = createDocumentType(fileTypes, comparator, getDocumentFromFiles(files))
                    }
                }
            }
        }

        return data
    }

    @WorkerThread
    private fun createDocumentType(fileTypes: List<FileType>, comparator: Comparator<Document>?, documents: MutableList<Document>): HashMap<FileType, List<Document>> {
        val documentMap = HashMap<FileType, List<Document>>()

        for (fileType in fileTypes) {
            val documentListFilteredByType = documents.filter { document -> FilePickerUtils.contains(fileType.extensions, document.mimeType) }

            comparator?.let {
                documentListFilteredByType.sortedWith(comparator)
            }

            documentMap[fileType] = documentListFilteredByType
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
            val title = fileitem.name;

            if (path != null && title != null) {
                val fileType = getFileType(PickerManager.getFileTypes(), path)
                val fileinfo = File(path)
                Log.e("VMSpecialPicker", path);
                Log.e("VMSpecialPicker", title);
                Log.e("VMSpecialPicker", fileType.toString());
                val document = title?.let { Document(imageId, it, fileitem.uri) }
                document.fileType = fileType
                document.size = fileinfo.length().toString()
                document.mtime = (fileinfo.lastModified() / 1000).toString()
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