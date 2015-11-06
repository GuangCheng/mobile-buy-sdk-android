/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Shopify Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.shopify.buy.ui.collections;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.shopify.buy.dataprovider.BuyClient;
import com.shopify.buy.dataprovider.CollectionsProvider;
import com.shopify.buy.model.Collection;
import com.shopify.buy.ui.common.BaseBuilder;
import com.shopify.buy.ui.common.BaseConfig;

import java.util.List;

public class CollectionListBuilder extends BaseBuilder<CollectionListBuilder> {

    /**
     * Create a default CollectionListBuilder.
     * If this constructor is used, {@link #setShopDomain(String)}, {@link #setApplicationName(String)}, {@link #setApiKey(String)}, {@link #setChannelid(String)}} must be called.
     *
     * @param context context to use for starting the {@code Activity}
     */
    public CollectionListBuilder(Context context) {
        super(context);
    }

    /**
     * Constructor that will use an existing {@link BuyClient} to configure the {@link CollectionListFragment}.
     *
     * @param context context to use for launching the {@code Activity}
     * @param client  the {@link BuyClient} to use to configure the CollectionListFragment
     */
    public CollectionListBuilder(Context context, BuyClient client) {
        super(context, client);
    }

    @Override
    protected BaseConfig getConfig() {
        if (config == null) {
            config = new CollectionListConfig();
        }
        return config;
    }

    public CollectionListBuilder setCollections(List<Collection> collections) {
        ((CollectionListConfig) config).setCollections(collections);
        return this;
    }

    public Bundle buildBundle() {
        // TODO looks like config should be generic in base, lets refactor the config so we can move this function up into the base
        CollectionListConfig collectionListConfig = (CollectionListConfig) config;

        Bundle bundle = super.buildBundle();
        bundle.putAll(collectionListConfig.toBundle());
        return bundle;
    }

    /**
     * Returns a new {@link CollectionListFragment} based on the params that have already been passed to the builder.
     *
     * @param provider  An optional implementation of {@link CollectionsProvider}. If you pass null, {@link com.shopify.buy.dataprovider.DefaultCollectionsProvider} will be used.
     * @param listener  An implementation of {@link com.shopify.buy.ui.collections.CollectionListFragment.Listener} which will be notified of user actions.
     * @return          A new {@link CollectionListFragment}.
     */
    public CollectionListFragment buildFragment(@Nullable CollectionsProvider provider, CollectionListFragment.Listener listener) {
        CollectionListFragment fragment = new CollectionListFragment();
        fragment.setProvider(provider);
        fragment.setListener(listener);
        fragment.setArguments(buildBundle());
        return fragment;
    }

}