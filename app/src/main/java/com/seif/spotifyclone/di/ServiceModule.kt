package com.seif.spotifyclone.di

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.seif.spotifyclone.data.remote.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideMusicDatabase() = MusicDatabase()

    // audio attributes which will save a meta information about our player
    @ServiceScoped // will have the same instance of these audio attributes in our same service instance
    @Provides
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context:Context,
        audioAttributes: AudioAttributes
    ) = ExoPlayer.Builder(context).apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true) // pause our media player if the user plugs in his headphones bec it could be so noisy to teh user
    }

    @ServiceScoped
    @Provides
    fun provideDataSourceFactory( // source to our data that will use to provide our actual music source later on so our music source will be our firebase source
        @ApplicationContext context: Context
    ) = DefaultDataSource.Factory(context)
// user Agent : a name in which the player can see who is actually connected to it
}
// DefaultDataSourceFactory(context, Util.getUserAgent(context,"Spotify App"))