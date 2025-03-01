package io.github.zyrouge.symphony.ui.view.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.zyrouge.symphony.services.groove.*
import io.github.zyrouge.symphony.services.radio.Radio
import io.github.zyrouge.symphony.ui.components.IconTextBody
import io.github.zyrouge.symphony.ui.helpers.RoutesBuilder
import io.github.zyrouge.symphony.ui.helpers.ViewContext
import io.github.zyrouge.symphony.utils.randomSubList
import io.github.zyrouge.symphony.utils.runIfTrueOrDefault
import io.github.zyrouge.symphony.utils.subListNonStrict

enum class ForYou(val label: (context: ViewContext) -> String) {
    Albums(label = { it.symphony.t.SuggestedAlbums }),
    Artists(label = { it.symphony.t.SuggestedArtists }),
    AlbumArtists(label = { it.symphony.t.SuggestedAlbumArtists })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouView(context: ViewContext) {
    val albumArtistsIsUpdating by context.symphony.groove.albumArtist.isUpdating.collectAsState()
    val albumsIsUpdating by context.symphony.groove.album.isUpdating.collectAsState()
    val artistsIsUpdating by context.symphony.groove.artist.isUpdating.collectAsState()
    val songsIsUpdating by context.symphony.groove.song.isUpdating.collectAsState()
    val albumArtistIds = context.symphony.groove.albumArtist.all
    val albumIds = context.symphony.groove.album.all
    val artistIds = context.symphony.groove.artist.all
    val songIds = context.symphony.groove.song.all
    val sortBy by context.symphony.settings.lastUsedSongsSortBy.collectAsState()
    val sortReverse by context.symphony.settings.lastUsedSongsSortReverse.collectAsState()

    when {
        songIds.isNotEmpty() -> {
            val sortedSongIds by remember {
                derivedStateOf {
                    runIfTrueOrDefault(!songsIsUpdating, listOf()) {
                        context.symphony.groove.song.sort(songIds.toList(), sortBy, sortReverse)
                    }
                }
            }
            val recentlyAddedSongs by remember(!songsIsUpdating) {
                derivedStateOf {
                    runIfTrueOrDefault(!songsIsUpdating, listOf()) {
                        context.symphony.groove.song.sort(
                            songIds.toList(),
                            SongSortBy.DATE_ADDED,
                            true
                        )
                    }
                }
            }
            val randomAlbums by remember {
                derivedStateOf {
                    runIfTrueOrDefault(!albumsIsUpdating, listOf()) {
                        albumIds.randomSubList(6)
                    }
                }
            }
            val randomArtists by remember {
                derivedStateOf {
                    runIfTrueOrDefault(!artistsIsUpdating, listOf()) {
                        artistIds.randomSubList(6)
                    }
                }
            }
            val randomAlbumArtists by remember {
                derivedStateOf {
                    runIfTrueOrDefault(!albumArtistsIsUpdating, listOf()) {
                        albumArtistIds.randomSubList(6)
                    }
                }
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.padding(20.dp, 0.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ForYouButton(
                            icon = Icons.Default.PlayArrow,
                            text = {
                                Text(context.symphony.t.PlayAll)
                            },
                            enabled = !songsIsUpdating,
                            onClick = {
                                context.symphony.radio.shorty.playQueue(sortedSongIds)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ForYouButton(
                            icon = Icons.Default.Shuffle,
                            text = {
                                Text(context.symphony.t.ShufflePlay)
                            },
                            enabled = !songsIsUpdating,
                            onClick = {
                                context.symphony.radio.shorty.playQueue(
                                    songIds.toList(),
                                    shuffle = true,
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                SideHeading {
                    Text(context.symphony.t.RecentlyAddedSongs)
                }
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    songsIsUpdating -> SixGridLoading()
                    recentlyAddedSongs.isEmpty() -> SixGridEmpty(context)
                    else -> BoxWithConstraints {
                        val tileWidth = maxWidth.times(0.7f)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Spacer(modifier = Modifier.width(12.dp))
                            recentlyAddedSongs.subListNonStrict(5).forEachIndexed { i, songId ->
                                val tileHeight = 96.dp
                                val backgroundColor = MaterialTheme.colorScheme.surface
                                val song = context.symphony.groove.song.get(songId)
                                    ?: return@forEachIndexed

                                ElevatedCard(
                                    modifier = Modifier
                                        .width(tileWidth)
                                        .height(tileHeight),
                                    onClick = {
                                        context.symphony.radio.shorty.playQueue(
                                            recentlyAddedSongs,
                                            options = Radio.PlayOptions(index = i),
                                        )
                                    }
                                ) {
                                    Box {
                                        AsyncImage(
                                            song.createArtworkImageRequest(context.symphony)
                                                .build(),
                                            null,
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.matchParentSize(),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            backgroundColor.copy(alpha = 0.2f),
                                                            backgroundColor.copy(alpha = 0.7f),
                                                            backgroundColor.copy(alpha = 0.8f),
                                                        ),
                                                    )
                                                )
                                        )
                                        Row(modifier = Modifier.padding(8.dp)) {
                                            Box {
                                                AsyncImage(
                                                    song.createArtworkImageRequest(context.symphony)
                                                        .build(),
                                                    null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(4.dp)),
                                                )
                                                Box(
                                                    modifier = Modifier.matchParentSize(),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                backgroundColor.copy(alpha = 0.25f),
                                                                CircleShape,
                                                            )
                                                            .padding(1.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PlayArrow,
                                                            null,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.Center,
                                            ) {
                                                Text(
                                                    song.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                song.artistName?.let {
                                                    Text(
                                                        it,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
                val contents by context.symphony.settings.forYouContents.collectAsState()
                contents.forEach {
                    when (it) {
                        ForYou.Albums -> SuggestedAlbums(
                            context,
                            isLoading = albumsIsUpdating,
                            albumIds = randomAlbums,
                        )

                        ForYou.Artists -> SuggestedArtists(
                            context,
                            label = context.symphony.t.SuggestedArtists,
                            isLoading = artistsIsUpdating,
                            artistIds = randomArtists,
                        )

                        ForYou.AlbumArtists -> SuggestedAlbumArtists(
                            context,
                            label = context.symphony.t.SuggestedAlbumArtists,
                            isLoading = albumArtistsIsUpdating,
                            albumArtistIds = randomAlbumArtists,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        else -> IconTextBody(
            icon = { modifier ->
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    modifier = modifier,
                )
            },
            content = { Text(context.symphony.t.DamnThisIsSoEmpty) },
        )
    }
}

@Composable
private fun SideHeading(text: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(20.dp, 0.dp)) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            text()
        }
    }
}

@Composable
private fun ForYouButton(
    icon: ImageVector,
    text: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ElevatedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            text()
        }
    }
}

@Composable
private fun SixGridLoading() {
    Box(
        modifier = Modifier
            .height((LocalConfiguration.current.screenHeightDp * 0.2f).dp)
            .fillMaxWidth()
            .padding(0.dp, 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SixGridEmpty(context: ViewContext) {
    val height = (LocalConfiguration.current.screenHeightDp * 0.15f).dp
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
            .padding(0.dp, 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            context.symphony.t.DamnThisIsSoEmpty,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun <T> StatedSixGrid(
    context: ViewContext,
    isLoading: Boolean,
    items: List<T>,
    content: @Composable (T) -> Unit,
) {
    when {
        isLoading -> SixGridLoading()
        items.isEmpty() -> SixGridEmpty(context)
        else -> SixGrid(items) {
            content(it)
        }
    }
}

@Composable
private fun <T> SixGrid(
    items: List<T>,
    content: @Composable (T) -> Unit,
) {
    val gap = 12.dp
    Row(
        modifier = Modifier.padding(20.dp, 0.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        (0..2).forEach { i ->
            val item = items.getOrNull(i)
            Box(modifier = Modifier.weight(1f)) {
                item?.let { content(it) }
            }
        }
    }
    if (items.size > 3) {
        Spacer(modifier = Modifier.height(gap))
        Row(
            modifier = Modifier.padding(20.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            (3..5).forEach { i ->
                val item = items.getOrNull(i)
                Box(modifier = Modifier.weight(1f)) {
                    item?.let { content(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedAlbums(
    context: ViewContext,
    isLoading: Boolean,
    albumIds: List<Long>,
) {
    val albums by remember {
        derivedStateOf {
            context.symphony.groove.album.get(albumIds)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SideHeading {
        Text(context.symphony.t.SuggestedAlbums)
    }
    Spacer(modifier = Modifier.height(12.dp))
    StatedSixGrid(context, isLoading, albums) { album ->
        Card(
            onClick = {
                context.navController.navigate(
                    RoutesBuilder.buildAlbumRoute(album.id)
                )
            }
        ) {
            AsyncImage(
                album.createArtworkImageRequest(context.symphony).build(),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedArtists(
    context: ViewContext,
    label: String,
    isLoading: Boolean,
    artistIds: List<String>,
) {
    val artists by remember {
        derivedStateOf {
            context.symphony.groove.artist.get(artistIds)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SideHeading {
        Text(label)
    }
    Spacer(modifier = Modifier.height(12.dp))
    StatedSixGrid(context, isLoading, artists) { artist ->
        Card(
            onClick = {
                context.navController.navigate(
                    RoutesBuilder.buildArtistRoute(artist.name)
                )
            }
        ) {
            AsyncImage(
                artist.createArtworkImageRequest(context.symphony).build(),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedAlbumArtists(
    context: ViewContext,
    label: String,
    isLoading: Boolean,
    albumArtistIds: List<String>,
) {
    val albumArtists by remember {
        derivedStateOf {
            context.symphony.groove.albumArtist.get(albumArtistIds)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SideHeading {
        Text(label)
    }
    Spacer(modifier = Modifier.height(12.dp))
    StatedSixGrid(context, isLoading, albumArtists) { albumArtist ->
        Card(
            onClick = {
                context.navController.navigate(
                    RoutesBuilder.buildAlbumArtistRoute(albumArtist.name)
                )
            }
        ) {
            AsyncImage(
                albumArtist.createArtworkImageRequest(context.symphony).build(),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}
