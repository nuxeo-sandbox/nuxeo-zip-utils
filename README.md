# nuxeo-zip-utils

This plugin brings utilities around zip files stored in a Document, and also can zip the content of a Folderish Document.


## Operations


### Files > `ZipUtils.IsZip`
* Input is `Document` or `Blob`
* Parameter: `xpath` ("file:content" by default)
* Return the input unchanged
* Set the `zipInfo_isZip` Context Variable to `true` or `false`

JS Example:

```
// In this example, input is a Document. The operation
// tests if file:content is a zip file
function run(input, params) {
  . . .
  ZipUtils.IsZip(input, {});
  // Valuer is returned as a boolean in the "zipInfo_isZip" Context variable:
  if(ctx.zipInfo_isZip) {
    . . .
  }
  . . .
}
```

### Files > `ZipUtils.EntriesList`
* Input is `Document` or `Blob`
* Parameter: `xpath` ("file:content" by default)
* Return the input unchanged
* Set the `zipInfo_entriesList` String Context Variable to the full list of all entries (one/line)


### Files > `ZipUtils.EntryInfo`
* Input is `Document` or `Blob`
* Parameters: `xpath` ("file:content" by default) and `entryName` (exact full path in the zip)
* Return the input unchanged
* Set several context int/long variables: `zipInfo_compressedSize`, `zipInfo_originalSize`, `zipInfo_crc`, and `zipInfo_method (0 = stored, 8 =  compressed)


### Files > `ZipUtils.GetFile`
* Input is `Document` or `Blob`
* Parameters: `xpath` ("file:content" by default) and `entryName` (exact full path in the zip)
* Returns the corresponding file. Return null if the entry does not exist or is a folder


### Files > `ZipUtils.UnzipToDocumentsOp`
* Input is `Document` or `Blob`
* Unzips the blob and creates the same structure in the target (the parent container when input is a Document and target is not provided).
* Returns the created folderish
* Parameters:
  * `xpath` (optional)
  * `target` (required if input is a blob): The parent document where to unzip
  * `folderishType` (optional): Type to use when creating a Folderish (default is Folder)
  * `commitModulo` (optional): Save and commit transaction regularly (strongly recommended to avoid transaction timeout when you know the zip contains a lot of files). Default is 100
  * `mainFolderishType` (optional): Type of the main container created to expand the zip (default is Folder)
  * `mainFolderishName` (optional): The name for this main container


### Files > `ZipUtils.ZipFolderishOp`
* Input is a Folderish document
* Zip all the content recursively, with the hierarchy. Ignore non-folderish documents that have no blobs
* Returns the Zipped content
* Parameters:
  * `callbackChain` (optional): Byt default, the blob is read in "file:content". Use a callback chain to tune this behavior. Your chain _must_  receive `Document` as input and must output a Blob (even if null)
  * `whereClauseOverride`: To find the children of folderish documents, the operation exludes by default the children that are HiddenInNavigation, version, proxy, or in the trash. You can define your own filter. WARNING: do not start it with "AND", the code prefixes it for you.
    The default is `ecm:mixinType != 'HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:isTrashed = 0`
  * `doNotCreateMainFolder` (optionl): When `true` the zip archive TOC will not start with the name of the main folder.


### Files > `ZipUtils.ZipInfo`
* Input is `Document` or `Blob`
* Returns the input unchanged
* Parameter: `xpath`, optional ("file:content" by default)
* Return info about the zip in Context Variables: `zipInfo_comment`, `zipInfo_countFiles` (int), `zipInfo_countDirectories` (int)


## Build and Install

Build with maven (at least 3.3)

```
cd /path/to/nuxeo-zip-utils
mvn clean install
# _> the package is in nuxeo-zip-utils-package/target
```

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).
