package droidninja.filepicker.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import java.io.File
import java.io.FilenameFilter
import java.util.*

@Suppress("unused")
class ScanDocumentFileUtil {

    companion object {
        //手机外部存储根目录 Mobile storage root directory
        val externalStorageDirectory: String by lazy {
            Environment.getExternalStorageDirectory().absolutePath
        }

        //手机app缓存存放路径 Mobile app cache storage path
        val android_app_data_folder: String by lazy {
            "$externalStorageDirectory/Android/data"
        }
    }
    private val PATH_NOT_EXISTS = -1L
    private var isStop = true
    private val mRootPath: String
    private var mCallBackFilter: FilenameFilter? = null
    private var mScanLevel = -1L
    private var mCoroutineScope: CoroutineScope? = null
    private var mCoroutineSize = 0
    private var mScanTime = 0L
    private var mScanFileListener: ScanFileListener? = null
    private var mContext:Context;

    constructor(rootPath: String, context:Context) {
        this.mContext = context;
        this.mRootPath = rootPath.trimEnd { it == '/' }
    }
//    constructor(rootPath: String, scanFileListener: ScanFileListener) {
//        this.mRootPath = rootPath.trimEnd { it == '/' }
//        mScanFileListener = scanFileListener
//    }
    fun setScanFileListener(scanFileListener: ScanFileListener) {
        mScanFileListener = scanFileListener
    }
    fun setScanLevel(level: Long) {
        mScanLevel = level
    }
    fun stop() {
        isStop = true
        mCoroutineScope?.cancel()
    }
    fun getScanTimeConsuming() = mScanTime
    fun startAsyncScan() {
        if (!isStop) {
            return
        }
        isStop = false
        mCoroutineSize = 0

        val file = fileUriUtils.getDoucmentTree(mContext, mRootPath)
        if (!file.exists()) {
            mScanFileListener?.scanComplete(PATH_NOT_EXISTS)
            return
        }

        if (mCoroutineScope == null || mCoroutineScope?.isActive == false) {
            mCoroutineScope = CoroutineScope(Dispatchers.IO)
        }
        mScanTime = System.currentTimeMillis()
        mScanFileListener?.scanBegin()
        asyncScan(file)
    }
    private fun asyncScan(dirOrFile: DocumentFile) {
        plusCoroutineSize()

//        mCoroutineScope?.launch(Dispatchers.IO) {
        run launch@ {
            if (checkLevel(dirOrFile)) {
                checkCoroutineSize()
                return@launch
            }

            if (dirOrFile.isFile) {
                if (filterFile(dirOrFile)) {
                    internalCallFile(dirOrFile)
                }
                checkCoroutineSize()
                return@launch
            }
            val rootFile = getFilterFilesList(dirOrFile)
            rootFile?.map {
                if (it.isDirectory) {
                    if (filterFile(it)) {
                        internalCallFile(it)
                    }
                    asyncScan(it)
                } else {
                    if (filterFile(it)) {
                        internalCallFile(it)
                    }
                }
            }
            checkCoroutineSize()
            return@launch
        }
    }
    @Synchronized
    private fun internalCallFile(dirOrFile: DocumentFile) {
        mScanFileListener?.scanningCallBack(dirOrFile)
    }
    @Synchronized
    private fun plusCoroutineSize() {
        mCoroutineSize++
    }
    @Synchronized
    private fun checkCoroutineSize() {
        mCoroutineSize--
        if (mCoroutineSize == 0) {
            isStop = true
            mCoroutineScope?.launch(Dispatchers.Main) {
                mScanTime = System.currentTimeMillis() - mScanTime
                mScanFileListener?.scanComplete(mScanTime)
                mCoroutineScope?.cancel()
            }

        }
    }
    private fun checkLevel(dirOrFile: DocumentFile): Boolean {
        if (mScanLevel != -1L) {
            var scanLevelCont = 0L
            dirOrFile.uri.path?.replace(mRootPath, "")?.map {
                if (it == '/') {
                    scanLevelCont++
                    if (scanLevelCont >= mScanLevel) {
                        return true
                    }
                }
            }
        }
        return false
    }
    private fun filterFile(file: DocumentFile): Boolean {
        return true;
//        return if (mCallBackFilter == null) {
//            !isStop
//        } else {
//            mCallBackFilter!!.accept(file, file.name) && !isStop
//        }
    }
    private fun getScanningTask(): CoroutineScope? {
        return mCoroutineScope
    }
    fun setCallBackFilter(filter: FilenameFilter?) {
        this.mCallBackFilter = filter
    }

    private fun getFilterFilesList(file: DocumentFile): Array<DocumentFile>? {
        file.uri.path?.let { Log.d("ScanDocumentFileUtil", it) }
        return file.listFiles();
    }

    class FileFilterBuilder {
        private val customFilterList: MutableList<FilenameFilter> = mutableListOf()
        private val mFilseFilterSet: MutableSet<String> = hashSetOf()
        private val mNameLikeFilterSet: MutableSet<String> = hashSetOf()
        private val mNameNotLikeFilterSet: MutableSet<String> = hashSetOf()

        /**
         * 是否扫描隐藏文件 true扫描 false不扫描
         */
        private var isScanHiddenFiles = true

        /**
         * 只要扫描文件
         * Just scan the file
         */
        private var isOnlyFile = false

        /**
         * 只扫描文件夹
         * Scan folders only
         */
        private var isOnlyDir = false


        /**
         * 添加自定义filter规则
         * Add custom filter rule
         */
        fun addCustomFilter(filter: FilenameFilter): FileFilterBuilder {
            customFilterList.add(filter)
            return this
        }

        /**
         * 只扫描文件夹
         * Scan folders only
         */
        fun onlyScanDir(): FileFilterBuilder {
            isOnlyDir = true
            return this
        }

        /**
         * 只要扫描文件
         * Just scan the file
         */
        fun onlyScanFile(): FileFilterBuilder {
            isOnlyFile = true
            return this
        }

        /**
         * 扫描名字像它的文件或者文件夹
         * Scan names like its files or folders
         */
        fun scanNameLikeIt(like: String): FileFilterBuilder {
            mNameLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
            return this
        }

        /**
         * 扫描名与其文件不同
         * 也就是说，不要扫描这样的文件
         * Scan name is not like its file
         * That is, don't scan files with names like this
         */
        fun scanNameNotLikeIt(like: String): FileFilterBuilder {
            mNameNotLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
            return this
        }

        /**
         * 扫描TxT文件
         * Scan text files only
         */
        fun scanTxTFiles(): FileFilterBuilder {
            mFilseFilterSet.add("txt")
            return this
        }

        /**
         * 不扫描隐藏文件
         * Don't scan hidden files
         */
        fun notScanHiddenFiles(): FileFilterBuilder {
            isScanHiddenFiles = false
            return this
        }

        /**
         *  扫描apk文件
         * Scan APK files
         */
        fun scanApkFiles(): FileFilterBuilder {
            mFilseFilterSet.add("apk")
            return this
        }


        /**
         * 扫描log文件 temp文件
         * Scan log file temp file
         */
        fun scanLogFiles(): FileFilterBuilder {
            mFilseFilterSet.add("log")
            mFilseFilterSet.add("temp")
            return this
        }

        /**
         * 扫描文档类型文件
         */
        fun scanDocumentFiles(): FileFilterBuilder {
//            mFilseFilterSet.add("txt")
            mFilseFilterSet.add("pdf")
            mFilseFilterSet.add("csv")
            mFilseFilterSet.add("doc")
            mFilseFilterSet.add("docx")
            mFilseFilterSet.add("xls")
            mFilseFilterSet.add("xlsx")
            mFilseFilterSet.add("ppt")
            mFilseFilterSet.add("pptx")
            return this
        }

        /**
         * 扫描图片类型文件
         *Scan picture type file
         */
        fun scanPictureFiles(): FileFilterBuilder {
            mFilseFilterSet.add("jpg")
            mFilseFilterSet.add("jpeg")
            mFilseFilterSet.add("png")
            mFilseFilterSet.add("bmp")
            mFilseFilterSet.add("gif")
            return this
        }

        /**
         * 扫描多媒体文件类型
         *Scan multimedia file type
         */
        fun scanVideoFiles(): FileFilterBuilder {
            mFilseFilterSet.add("mp4")
            mFilseFilterSet.add("avi")
            mFilseFilterSet.add("wmv")
            mFilseFilterSet.add("flv")
            return this
        }

        /**
         * 扫描音频文件类型
         * Scan audio file type
         */
        fun scanMusicFiles(): FileFilterBuilder {
            mFilseFilterSet.add("mp3")
            mFilseFilterSet.add("ogg")
            return this
        }

        /**
         * 扫描压缩包文件类型
         */
        fun scanZipFiles(): FileFilterBuilder {
            mFilseFilterSet.add("zip")
            mFilseFilterSet.add("rar")
            mFilseFilterSet.add("7z")
            return this
        }

        /**
         * 检查名字相似过滤
         */
        private fun checkNameLikeFilter(name: String): Boolean {
            //相似名字获取过滤
            if (mNameLikeFilterSet.isNotEmpty()) {
                mNameLikeFilterSet.map {
                    if (name.toLowerCase(Locale.getDefault()).contains(it)) {
                        return true
                    }
                }
                return false
            }
            return true
        }

        /**
         * 检查名字不相似过滤
         */
        private fun checkNameNotLikeFilter(name: String): Boolean {
            //名字不相似顾虑
            if (mNameNotLikeFilterSet.isNotEmpty()) {
                mNameNotLikeFilterSet.map {
                    if (name.toLowerCase(Locale.getDefault()).contains(it)) {
                        return false
                    }
                }
                return true
            }
            return true
        }

        /**
         * 检查文件后缀过滤规则 既文件类型过滤规则
         */
        private fun checkSuffixFilter(name: String): Boolean {
            return if (mFilseFilterSet.isNotEmpty()) {
                //获取文件后缀
                val suffix: String =
                    name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                        .toLowerCase(Locale.getDefault())
                //return 是否包含这个文件
                mFilseFilterSet.contains(suffix)
            } else {
                //如果没有设置这个规则，全部默认为true 全部通过
                true
            }
        }

        /**
         * 重置构建器
         */
        fun resetBuild() {
            mFilseFilterSet.clear()
            mNameLikeFilterSet.clear()
            mNameNotLikeFilterSet.clear()
            customFilterList.clear()
            isScanHiddenFiles = true
            isOnlyDir = false
            isOnlyFile = false
        }

        /**
         * 创建过滤规则
         * Create filter rule
         */
        fun build(): FilenameFilter {
            return object : FilenameFilter {

                override fun accept(dir: File, name: String): Boolean {
                    //先检查用户自定义的过滤规则 优先级最高
                    if (customFilterList.isNotEmpty()) {
                        for (filenameFilter in customFilterList) {
                            val accept = filenameFilter.accept(dir, name)
                            if (!accept) {
                                //只要有一个过滤规则不通过就停止循环
                                return false
                            }
                        }
                    }

                    //隐藏文件扫描规则 优先级高
                    // isScanHiddenFiles==true 扫描隐藏文件就不判断是不是隐藏文件了
                    // isScanHiddenFiles==false 不扫描隐藏文件 判断是不是隐藏文件 是隐藏文件就过滤
                    if (!isScanHiddenFiles && dir.isHidden) {
                        return false
                    }

                    //只扫描文件夹 文件夹不需要后缀规则检查
                    if (isOnlyDir) {
                        return dir.isDirectory
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                    }

                    //只扫描文件 同时应用文件扫描规则
                    if (isOnlyFile) {
                        return dir.isFile
                                && checkSuffixFilter(name)
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                    }

                    //默认检查规则
                    return checkSuffixFilter(name)
                            && checkNameLikeFilter(name)
                            && checkNameNotLikeFilter(name)
                }
            }

        }

    }

    /**
     * 扫描文件监听器
     * Scanning file listener
     */
    interface ScanFileListener {

        /**
         * 在子线程回调
         * Callback in child thread
         * 扫描开始的时候 描述
         */
        fun scanBegin()

        /**
         * 在主线程回调
         * Callback in main thread
         * 扫描完成回调 Scan completion callback
         * 同时也是异常处理回调，取决于返回时间如果是负值的话
         * @param timeConsuming 耗时 时间为-1说明扫描目录不存在
         */
        fun scanComplete(timeConsuming: Long)

        /**
         * 在子线程回调
         * 扫描到文件时回调，每扫描到一个文件触发一次
         * Callback in child thread
         * Callback when a file is scanned, triggered every time a file is scanned
         * @param file 扫描的文件
         */
        fun scanningCallBack(file: DocumentFile)
    }

}