LazyList
========

# Android Lazy List component

## Version

1.0.0

### Description

LazyList component for Android. ListView which supports lazy loading of resources (data), either
synchronously (from memory, from local DB...) or asynchronously (HTTP, REST, ...).

Motivation is, that even when you have the data available in the memory, once you are porting it to
listView adapter, it can get really slow to initialize the listView with large amount of data (200+ items).
Speeding this process up is really easy with **LazyList**, because you can first initialize the adapter with
only small amount of data, but later add all items as user will scroll the list down.

```
public class MovieListFragment extends LazyList<Movie> { ... }
```

```
public class MovieListAdapter extends LazyListAdapter<Movie> { ... }
```

Another case can be using a REST service to load the data. If you have a REST service which supports paging
(you should have :) ) it is easy to override `loadPage` method to call REST service directly. Just
define success/error callbacks and new data will automatically refresh the list.


    @Override
    protected synchronized void loadPage(final int page) {
        super.loadPage(page);

        ResourceService.loadMovies(page, new ServerResponseListener<Movie[]>() {
            @Override
            public void onSuccess(Movie[] movies) {
                if (movies.length > 0) {
                    mData.addAll(Arrays.asList(movies));
                    onDataLoadingCompleted(page);

                    // save data to DB (handles create/update)
                    MovieProvider.saveMoviesToDb(mData);
                } else {
                    onDataLoadingFailed(REASON_LIST_END, null);
                }
            }

            @Override
            public void onError(ErrorResponse error) {
                onDataLoadingFailed(LazyList.REASON_SERVER_ERROR, page);
            }
            ...
        });
    }


### Usage

Just import `com.doodeec.lazylist-X.Y.Z.aar` into your project and override adapter and fragment to
use desired types (you can look at the demo - [Filmster project](http://github.com/doodeec/filmster)
where it is used).

    dependencies {
        compile(name:'com.doodeec.lazylist-1.0.0', ext:'aar')
        ...
    }

### Support

Supports generic types - you can extend both adapter and fragment to use any kind of data
Supports Android SDK API10+ (2.3+)

### Licence

released under Apache 2.0 licence

### Demo

Check out [Filmster project](http://github.com/doodeec/filmster) to see the demo project which
fully takes advantage of **LazyList** library

### Author

[Dusan Bartos](http://doodeec.com)
In case of any questions, do not hesitate to contact me - [doodeec@gmail.com](mailto:doodeec@gmail.com)
