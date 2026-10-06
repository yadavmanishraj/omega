package com.manishraj.saavnmusic.ui.viewmodel
import androidx.lifecycle.*; import com.manishraj.saavnmusic.data.repository.MusicRepository; import com.manishraj.saavnmusic.data.settings.*; import com.manishraj.saavnmusic.domain.*; import com.manishraj.saavnmusic.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel; import kotlinx.coroutines.flow.*; import kotlinx.coroutines.launch; import javax.inject.Inject
@HiltViewModel class HomeViewModel @Inject constructor(private val repo:MusicRepository): ViewModel(){
    val trending=MutableStateFlow<UiState<List<Song>>>(UiState.Loading); val albums=MutableStateFlow<List<Album>>(emptyList()); val playlists=MutableStateFlow<List<Playlist>>(emptyList()); val artists=MutableStateFlow<List<Artist>>(emptyList()); val history:StateFlow<List<com.manishraj.saavnmusic.data.local.HistoryEntity>> = repo.history.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    init{ load() }
    fun load(){ viewModelScope.launch{ trending.value=UiState.Loading; try{ // Home = global search carousels for evergreen terms (the API has no public trending/home endpoint in the current controller set - see STUDY.md)
        trending.value=UiState.Success(repo.searchSongs("trending bollywood")); albums.value=repo.searchAlbums("latest albums"); playlists.value=repo.searchPlaylists("top playlists"); artists.value=repo.searchArtists("top singers") }catch(e:Exception){ trending.value=UiState.Error(e.message?:"Network error") } } }
}
@HiltViewModel class SearchViewModel @Inject constructor(private val repo:MusicRepository): ViewModel(){
    val query=MutableStateFlow(""); val tab=MutableStateFlow(0); val songs=MutableStateFlow<UiState<List<Song>>>(UiState.Success(emptyList())); val albums=MutableStateFlow<List<Album>>(emptyList()); val artists=MutableStateFlow<List<Artist>>(emptyList()); val playlists=MutableStateFlow<List<Playlist>>(emptyList()); val recent=repo.recentSearches.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    fun search(q:String){ query.value=q; if(q.isBlank()) return; viewModelScope.launch{ songs.value=UiState.Loading; repo.addRecentSearch(q); try{ songs.value=UiState.Success(repo.searchSongs(q)); albums.value=repo.searchAlbums(q); artists.value=repo.searchArtists(q); playlists.value=repo.searchPlaylists(q) }catch(e:Exception){ songs.value=UiState.Error(e.message?:"Search failed") } } }
    fun clearRecent(){ viewModelScope.launch{ repo.clearRecentSearches() } }
}
@HiltViewModel class DetailViewModel @Inject constructor(private val repo:MusicRepository): ViewModel(){
    val album=MutableStateFlow<UiState<Album>>(UiState.Loading); val playlist=MutableStateFlow<UiState<Playlist>>(UiState.Loading); val artist=MutableStateFlow<UiState<Artist>>(UiState.Loading)
    fun loadAlbum(id:String){ viewModelScope.launch{ album.value=UiState.Loading; try{album.value=UiState.Success(repo.album(id))}catch(e:Exception){album.value=UiState.Error(e.message?:"Failed")} } }
    fun loadPlaylist(id:String){ viewModelScope.launch{ playlist.value=UiState.Loading; try{playlist.value=UiState.Success(repo.playlist(id))}catch(e:Exception){playlist.value=UiState.Error(e.message?:"Failed")} } }
    fun loadArtist(id:String){ viewModelScope.launch{ artist.value=UiState.Loading; try{artist.value=UiState.Success(repo.artist(id))}catch(e:Exception){artist.value=UiState.Error(e.message?:"Failed")} } }
    fun lyrics(id:String, onResult:(String?)->Unit){ viewModelScope.launch{ onResult(repo.songWithLyrics(id)?.lyrics?.lyrics) } }
}
@HiltViewModel class LibraryViewModel @Inject constructor(private val repo:MusicRepository): ViewModel(){
    val favorites=repo.favorites.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList()); val downloads=repo.downloads.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList()); val history=repo.history.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList()); val playlists=repo.localPlaylists.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val sortDescending=MutableStateFlow(true)
    fun createPlaylist(name:String){ viewModelScope.launch{ repo.createPlaylist(name) } }; fun deletePlaylist(id:Long){ viewModelScope.launch{ repo.deletePlaylist(id) } }; fun clearHistory(){ viewModelScope.launch{ repo.clearHistory() } }; fun deleteDownload(id:String){ viewModelScope.launch{ repo.deleteDownload(id) } }
    fun addToPlaylist(pid:Long,s:Song){ viewModelScope.launch{ repo.addToPlaylist(pid,s) } }
}
@HiltViewModel class PlayerViewModel @Inject constructor(val player:PlayerController, private val repo:MusicRepository, private val settings:SettingsRepository): ViewModel(){
    val state=player.state; val appSettings=settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),AppSettings())
    fun play(songs:List<Song>, index:Int){ val s=songs.getOrNull(index) ?: return; viewModelScope.launch{ repo.recordPlay(s) }; player.playQueue(songs,index,appSettings.value.streamQuality) }
    fun isFavorite(id:String)=repo.isFavorite(id)
    fun toggleFavorite(s:Song, fav:Boolean){ viewModelScope.launch{ repo.toggleFavorite(s,fav) } }
    fun suggestions(id:String){ viewModelScope.launch{ try{ val extra=repo.suggestions(id); if(extra.isNotEmpty()) player.playQueue(extra,0,appSettings.value.streamQuality) }catch(_:Exception){} } }
}
@HiltViewModel class SettingsViewModel @Inject constructor(private val settings:SettingsRepository): ViewModel(){
    val state=settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),AppSettings())
    fun update(t:(AppSettings)->AppSettings){ viewModelScope.launch{ settings.update(t, state.value) } }
}
