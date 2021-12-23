package droidninja.filepicker.viewmodels

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.PickerManager
import droidninja.filepicker.models.Document
import droidninja.filepicker.models.FileType
import droidninja.filepicker.utils.FilePickerUtils
import droidninja.filepicker.utils.StorageTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

//其他应用存储目录的文件查询
class VMSpecialPicker(application: Application) : BaseViewModel(application) {

    private val _lvDocData = MutableLiveData<HashMap<FileType, List<Document>>>()
    val lvDocData: LiveData<HashMap<FileType, List<Document>>>
        get() = _lvDocData


    fun getDocs(fileTypes: List<FileType>, comparator: Comparator<Document>?) {
        launchDataLoad {
            val dirs = queryDocs(fileTypes, comparator)
            _lvDocData.postValue(dirs)
        }
    }

    @WorkerThread
    suspend fun queryDocs(fileTypes: List<FileType>, comparator: Comparator<Document>?): HashMap<FileType, List<Document>> {
        var data = HashMap<FileType, List<Document>>()
        withContext(Dispatchers.IO) {
            val exdir = "/Android/data/com.tencent.mm/MicroMsg/Download/"
            val files = StorageTool.getFiles(getApplication<Application>().applicationContext, exdir)

            if(files.isNotEmpty()){
                data = createDocumentType(fileTypes, comparator, getDocumentFromFiles(files))
            }
        }
        return data
    }

    @WorkerThread
    private fun createDocumentType(fileTypes: List<FileType>, comparator: Comparator<Document>?, documents: MutableList<Document>): HashMap<FileType, List<Document>> {
        val documentMap = HashMap<FileType, List<Document>>()

        for (fileType in fileTypes) {
            val documentListFilteredByType = documents.filter { document -> FilePickerUtils.containExt(fileType.extensions, document.path) }
            comparator?.let {
                documentListFilteredByType.sortedWith(comparator)
            }

            documentMap[fileType] = documentListFilteredByType
        }

        return documentMap
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