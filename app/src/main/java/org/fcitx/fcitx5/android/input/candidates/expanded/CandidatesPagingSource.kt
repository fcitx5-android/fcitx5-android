package org.fcitx.fcitx5.android.input.candidates.expanded

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.fcitx.fcitx5.android.daemon.FcitxConnection

class CandidatesPagingSource(val fcitx: FcitxConnection, val total: Int, val offset: Int) :
    PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        val pageNum = params.key ?: 0
        val pageSize = params.loadSize
        val candidates = fcitx.runOnReady {
            getCandidates(offset + pageNum * pageSize, pageSize)
        }
        val prevPage = if (pageNum > 0) pageNum - 1 else null
        val nextPage = if ((pageNum + 1) * pageSize >= total) null else pageNum + 1
        return LoadResult.Page(candidates.toList(), prevPage, nextPage)
    }

    // always reload from beginning
    override fun getRefreshKey(state: PagingState<Int, String>) = 0

}
