package com.seif.spotifyclone.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.seif.spotifyclone.R
import com.seif.spotifyclone.adapters.SongAdapter
import com.seif.spotifyclone.exoplayer.MusicServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSongAdapter(
        @ApplicationContext context: Context
    ) = SongAdapter(provideGlideInstance(context))

    @Singleton
    @Provides
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context
    ) = MusicServiceConnection(context)

    @Singleton // to use the same instance over and over again
    @Provides
    fun provideGlideInstance( // pass the objects we need to create this instance
       @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.ic_placholder)
            .error(R.drawable.ic_placholder)
            .diskCacheStrategy(DiskCacheStrategy.DATA) // to make sure that our images are cashed with glide
    )
}