# Trestle Changelog

## 0.8.2

This is a fairly small release with only minor changes to the Java backend, but some pretty significant improvements the web UI,
we strongly recommend upgrading to this release if you depend on the UI to properly function.

### Breaking changes
There shouldn't be any breaking changes in this release; however, the ```TrestleReasoner``` interface was refactored any many of the existing methods are now located on domain specific interfaces, which the main interface extends.
If you were relying on ```TrestleReasoner``` to directly declare those methods, you may have a problem. Otherwise, everything should work as expected.

### Improvements

#### Backend
The backend code underwent a significant refactoring with most of the functionality extracted from ```TrestleReasonerImpl``` and moved into specified *engine* modules,
which are the injected into reasoner as needed.

Last month, we updated our Sonar instllation to the 7.0 branch, which generated a huge number of additional warnings and flags.
So we took some time to clean those up and make some improvements.
There weren't any radical changes, but we did address a number of auto-boxing warnings and improperly closed file handles.
Always nice to tighten the screws.

#### UI

The web UI underwent a number of tweaks in this release. Aside some some squashed bugs, there shouldn't be much visible to the user, but behind the scenese, things are different.

##### Testing

We now have some simple *end-to-end* tests running on each pull request and development commit.
We've already identified and addressed a number of really annoying bugs (such as the application not loading correctly on the first try) and hopefully this will help us keep things moving along.
We're currently running the tests against Firefox on the CI server, since Chromium is having some problems.
Which means we fixed some of the scaling issues in Firefox.

#### Performance and build

The biggest change in this release, is that we completely revamped the application bundling process to dramatic effect.
Our development builds saw a 31.65% decrease in size (from 17.11MB to 11.69MB) and our production builds went down 42.86% (from 9.51MB to 5.43MB).
That's still not great, but much better than it was.

We also now do much better code splitting and only load polyfills as required by the brower, which we check at runtime. The final bundling improvement is that we now pre-build gzip and brotli compressed assets, and serve those based on the appropriate request headers.
The Brotli files are 20% smaller than gzip for our production build (982.9K vs 1.24MB). Nice!


Rounding out the UI changes are some small performance improvements. We now use a proper cache when stashing our individuals, instead of the homegrown ```Map``` implementation.
We also employ some caching within the ```TrestleIndividual``` class to avoid having to constantly search for spatial members or recompute the IDs.

Finally, we've implemented a simple web worker to handle some of the GeoJSON processing, so we don't block the main thread.
Not really that useful right now, but a good start.  


### Completed Tickets
    
<h4>        New Feature
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-629'>TRESTLE-629</a>] -         Add permissions test
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-630'>TRESTLE-630</a>] -         Add user create/delete/modify test
</li>
</ul>
    
<h4>        Task
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-619'>TRESTLE-619</a>] -         Resolve Sonar warnings
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-620'>TRESTLE-620</a>] -         Upgrade Hadoop to 3.0
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-627'>TRESTLE-627</a>] -         Implement E2E testing in Bamboo
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-662'>TRESTLE-662</a>] -         Improve UI build process
</li>
</ul>
    
<h4>        Improvement
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-466'>TRESTLE-466</a>] -         Move Concept code into its own engine
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-480'>TRESTLE-480</a>] -         Migrate TrestleObject code to its own module
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-567'>TRESTLE-567</a>] -         Add jsondoc strings
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-568'>TRESTLE-568</a>] -         Address Javadoc warnings
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-616'>TRESTLE-616</a>] -         Optimize bundling
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-617'>TRESTLE-617</a>] -         Conditionally load pollyfills
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-632'>TRESTLE-632</a>] -         Move exporter code into its own engine
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-639'>TRESTLE-639</a>] -         Reasoner Prefix binding should be an annotation
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-647'>TRESTLE-647</a>] -         Individual Service should use a proper cache, not a Map
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-648'>TRESTLE-648</a>] -         IndividualService should be a worker
</li>
</ul>
    
<h4>        Bug
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-626'>TRESTLE-626</a>] -         Login page tests are broken
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-642'>TRESTLE-642</a>] -         ExpressionChangedAfterItHasBeenCheckedError seems to be blocking window loading
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-644'>TRESTLE-644</a>] -         E2E tests are failing on Firefox
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-645'>TRESTLE-645</a>] -         Login/Logout status is not refreshing correctly
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-646'>TRESTLE-646</a>] -         Admin/DBA permissions are not working correctly
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-656'>TRESTLE-656</a>] -         Dataset viewer is not drawing map labels correctly.
</li>
</ul>
            
<h4>        Epic
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-624'>TRESTLE-624</a>] -         Reorganize internal engines
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-625'>TRESTLE-625</a>] -         Implement some basic E2E testing
</li>
</ul>
  


## 0.8.1

This release is a hot-fix to address some regressions introduced by the new Clojure parser.

### Breaking changes

While this release was supposed to be a minor bug fix, it ended up being much more.
The issue has to do with matching up facts with their corresponding class constructor parameters.
The old Java parser was horribly inconsistent and the fact that it even worked at all is kind of a miracle.

In order to get everything working again we had to make some serious breaking changes, 
so one option going forward is to simply disable the Clojure parser until you're able to upgrade by setting the following option: 

```yaml
trestle {
  useClojureParser: false
}
```

_Note: Due to the breaking changes in the Clojure parser, 
the old Java parser is not able to be updated to fully pass the integration tests.
As of now, the old parser passing 90+% of the test suite, 
but in order to maintain backwards compatibility, we can't backport our new changes.
The parser is scheduled to be removed in 0.10, see [this ticket](https://development.nickrobison.com:2096/jira/browse/TRESTLE-653) for details._  

In general, the changes are really much better and more defensible. 
The logic for matching a class member (either a field or a method goes like this):

1. If an annotation specifies a `name()` parameter, we use that for the constructor parameter name as well.
2. If no annotation is specified, we use the filtered Java member name. 
For fields, that means using the name directly. For methods, we remove the `get` from the name and lowercase the first letter from the resulting string

The one exception is for `@Language` annotations, since these essentially override the fact name, 
we default to using the Java member name via the filtering rules specified above.

In 0.9, we plan to add the ability to specify the constructor parameter name, see [this ticket](https://development.nickrobison.com:2096/jira/browse/TRESTLE-654) for details.

### Completed tickets

<h4>        Bug
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-384'>TRESTLE-384</a>] -         ClassParser is getting confused with fact and getter names
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-640'>TRESTLE-640</a>] -         Clojure Parser is causing Hadoop Integration Tests to fail
</li>
</ul>
            

## 0.8.0

Everything before 0.8 should be considered pre-alpha and left to wither in the sands of time.

0.8.0 is a pretty massive release with almost every part of the application being touched in some way.

The most visible changes:

* A totally redone UI that's much cleaner and more full featured.
* 
* Caching of Trestle Objects now works correctly and significantly reduces network round-trips to the database.
* Class Parser has been rewritten in Clojure and now performs the caching at startup so as to reduce the time it takes to parse and build objects.
* Significant performance and stability improvements.

### Breaking Changes

A lot, don't use anything that came before this, it's total garbage. 

### Completed tickets
    
<h4>        New Feature
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-495'>TRESTLE-495</a>] -         Add Individual Event Graph
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-539'>TRESTLE-539</a>] -         Add context logging
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-554'>TRESTLE-554</a>] -         Add Clojure Parser Feature Flag
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-564'>TRESTLE-564</a>] -         Add Swagger documentation to API
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-570'>TRESTLE-570</a>] -         Index Visualization
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-590'>TRESTLE-590</a>] -         Implement CSP
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-614'>TRESTLE-614</a>] -         Add UI error logging
</li>
</ul>
    
<h4>        Task
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-273'>TRESTLE-273</a>] -         Rewrite ClassParser in Clojure
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-276'>TRESTLE-276</a>] -         Add runtime class loading/unloading support
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-309'>TRESTLE-309</a>] -         Move navigation options from menu to sidenav
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-442'>TRESTLE-442</a>] -         Remove Oracle Jars
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-453'>TRESTLE-453</a>] -         Remove Agrona queue.
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-496'>TRESTLE-496</a>] -         Upgrade to Mapbox 0.41.0
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-602'>TRESTLE-602</a>] -         Upgrade dependencies for 0.8 release
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-603'>TRESTLE-603</a>] -         Run checker for 0.8 release
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-611'>TRESTLE-611</a>] -         Move FastTuple to Maven artifact
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-613'>TRESTLE-613</a>] -         Migrate vendored jars to maven repo
</li>
</ul>
    
<h4>        Improvement
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-15'>TRESTLE-15</a>] -         Add error handling to type constructor registration
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-41'>TRESTLE-41</a>] -         Build robust class registry
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-228'>TRESTLE-228</a>] -         MissingConstructorArgument exception is unclear
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-310'>TRESTLE-310</a>] -         Split user functions into their own module
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-311'>TRESTLE-311</a>] -         Combine user/authentication functions into shared module
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-312'>TRESTLE-312</a>] -         Create shared module for graphs and ui objects
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-313'>TRESTLE-313</a>] -         Add TrestleObject map demo
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-314'>TRESTLE-314</a>] -         Add individual visualization as a route
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-315'>TRESTLE-315</a>] -         Move search into its own component
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-316'>TRESTLE-316</a>] -         Add map to individual visualization
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-318'>TRESTLE-318</a>] -         Create explore module for non-admin users
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-341'>TRESTLE-341</a>] -         Add simple E2E testing
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-342'>TRESTLE-342</a>] -         Migrate User/Login forms to ReactiveForms
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-343'>TRESTLE-343</a>] -         Revamp Login screen
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-351'>TRESTLE-351</a>] -         Login/logout iconography is really bad
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-363'>TRESTLE-363</a>] -         Add icons and thin-style sidenav to current sidenav
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-371'>TRESTLE-371</a>] -         Make Login screen a modal
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-372'>TRESTLE-372</a>] -         Setup Production Pipeline
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-373'>TRESTLE-373</a>] -         Enable Angular AOT
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-391'>TRESTLE-391</a>] -         Cache should support database temporals
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-392'>TRESTLE-392</a>] -         Improve cache mocking
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-445'>TRESTLE-445</a>] -         Improve interaction of Checker and Generics
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-456'>TRESTLE-456</a>] -         Type Constructor should take a more specific generic function
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-464'>TRESTLE-464</a>] -         Remove Sesame Repository Connection Manager
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-499'>TRESTLE-499</a>] -         Add component_of relationship
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-504'>TRESTLE-504</a>] -         Drop console output in production build
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-507'>TRESTLE-507</a>] -         IndividualService should cache values
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-508'>TRESTLE-508</a>] -         TrestleMap should support disabling zoom on load
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-509'>TRESTLE-509</a>] -         Add support for increasing/decreasing spacing between objects
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-510'>TRESTLE-510</a>] -         Add support for disabling/removing individuals
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-511'>TRESTLE-511</a>] -         Add spatial intersection support
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-512'>TRESTLE-512</a>] -         Add support for exporting datasets
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-513'>TRESTLE-513</a>] -         Add Compare functionality
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-514'>TRESTLE-514</a>] -         Individuals should link to Visualize page
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-518'>TRESTLE-518</a>] -         Migrate Spatial Intersection to TrestleIndividuals
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-519'>TRESTLE-519</a>] -         Rewrite ClassBuilder in Clojure
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-522'>TRESTLE-522</a>] -         Migrate Spatial caches to TrestleCache
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-523'>TRESTLE-523</a>] -         Improve TrestleIndividual Caching
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-526'>TRESTLE-526</a>] -         Add GeoJSON export support
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-527'>TRESTLE-527</a>] -         Add KML export support
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-531'>TRESTLE-531</a>] -         Integrator should use SpatialEngine
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-535'>TRESTLE-535</a>] -         Migrate RxJS to lettable operators
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-541'>TRESTLE-541</a>] -         Add progress bar to Compare page
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-547'>TRESTLE-547</a>] -         Migrate RDF4J Dependencies to BOM
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-548'>TRESTLE-548</a>] -         Upgrade GraphDB to 8.4.x
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-558'>TRESTLE-558</a>] -         Recover underlying exception from Completion and Execution Exceptions
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-560'>TRESTLE-560</a>] -         Implement Temporal Engine
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-562'>TRESTLE-562</a>] -         TDTree should use switch statements instead of ifs
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-580'>TRESTLE-580</a>] -         Compare should have a Focus feature
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-584'>TRESTLE-584</a>] -         Object Relation table should have Visualize links
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-585'>TRESTLE-585</a>] -         Visualize page should have link to Compare page
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-586'>TRESTLE-586</a>] -         Visualize page should have a tab to display any split/merges, if they exist
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-592'>TRESTLE-592</a>] -         Spatial Engine should use injected Geometry Cache
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-593'>TRESTLE-593</a>] -         Extract readTrestleObject comparators as static methods 
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-612'>TRESTLE-612</a>] -         Cast FastTuple to implemented interfaces
</li>
</ul>
    
<h4>        Bug
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-270'>TRESTLE-270</a>] -         Registered classes don&#39;t always implement serializable
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-347'>TRESTLE-347</a>] -         Cleanup silly UI bugs
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-359'>TRESTLE-359</a>] -         Cache deadlocking on remove
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-383'>TRESTLE-383</a>] -         ClassParser fails when given more values than specified in the constructor
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-483'>TRESTLE-483</a>] -         Read/Write Trestle Object methods are not correctly wrapped
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-487'>TRESTLE-487</a>] -         Integration algorithm is failing on cluster
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-492'>TRESTLE-492</a>] -         Metrics page fails if Metrician is disabled
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-494'>TRESTLE-494</a>] -         IndividualGraph doesn&#39;t work with future dates
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-498'>TRESTLE-498</a>] -         D3 Graphs are drawing new Axis over existing Axis.
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-534'>TRESTLE-534</a>] -         Individuals should not use anchored names
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-540'>TRESTLE-540</a>] -         Angular Lazy Loading is Broken with NGC
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-542'>TRESTLE-542</a>] -         Visualize page isn&#39;t always mapping individuals
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-543'>TRESTLE-543</a>] -         Viewer doesn&#39;t allow for removing layers from the map
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-544'>TRESTLE-544</a>] -         Viewer is not getting cached object values
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-549'>TRESTLE-549</a>] -         Cache Eviction is throwing an Exception
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-559'>TRESTLE-559</a>] -         Cache does not handle overflowing values
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-563'>TRESTLE-563</a>] -         Cache Mock tests are failing when running the full integration suite
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-579'>TRESTLE-579</a>] -         Explode bar does not reset correctly
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-581'>TRESTLE-581</a>] -         Filter overlapping doesn&#39;t work anymore
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-588'>TRESTLE-588</a>] -         UnregisteredTransaction exception is back
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-591'>TRESTLE-591</a>] -         User DAO is invalid
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-599'>TRESTLE-599</a>] -         Build is failing with unreachable endpoints
</li>
</ul>
    
<h4>        Sub-task
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-348'>TRESTLE-348</a>] -         Editing an existing user shouldn&#39;t result in another user being added to the table
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-349'>TRESTLE-349</a>] -         Gravitar image is a weird/sloppy size
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-361'>TRESTLE-361</a>] -         Add user button should be right aligned
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-362'>TRESTLE-362</a>] -         Map Highlighting should not cover boundary lines
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-400'>TRESTLE-400</a>] -         Enabled strict mode in tsconfig
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-497'>TRESTLE-497</a>] -         Selected Individual should be centered on the graph
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-502'>TRESTLE-502</a>] -         Nodes should have hover text
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-506'>TRESTLE-506</a>] -         Add Y-axis labels
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-516'>TRESTLE-516</a>] -         Figure out how to draw links between component_with individuals
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-571'>TRESTLE-571</a>] -         Index should calculate its own triangle points
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-572'>TRESTLE-572</a>] -         Add index rebuild task
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-573'>TRESTLE-573</a>] -         Add cache busting functionality
</li>
</ul>
        
<h4>        Epic
</h4>
<ul>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-272'>TRESTLE-272</a>] -         Total rewrite/reimplementation of the class parser/register
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-306'>TRESTLE-306</a>] -         Revamp UI to split user/admin functions and modularize shared components.
</li>
<li>[<a href='https://development.nickrobison.com:2096/jira/browse/TRESTLE-493'>TRESTLE-493</a>] -         Implement Spatial Comparison Tab in Web UI
</li>
</ul>
