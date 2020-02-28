# About

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-zip-utils-master)](https://qa.nuxeo.org/jenkins/job/Sandbox/job/sandbox_nuxeo-zip-utils-master/)

Utilities for dealing with archives (zip, tar, rar, etc.) in Nuxeo.

# Operations


## Files > `ZipUtils.IsZip`
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

## Files > `ZipUtils.EntriesList`
* Input is `Document` or `Blob`
* Parameter: `xpath` ("file:content" by default)
* Return the input unchanged
* Set the `zipInfo_entriesList` String Context Variable to the full list of all entries (one/line)


## Files > `ZipUtils.EntryInfo`
* Input is `Document` or `Blob`
* Parameters: `xpath` ("file:content" by default) and `entryName` (exact full path in the zip)
* Return the input unchanged
* Set several context int/long variables: `zipInfo_compressedSize`, `zipInfo_originalSize`, `zipInfo_crc`, and `zipInfo_method (0 = stored, 8 =  compressed)


## Files > `ZipUtils.GetFile`
* Input is `Document` or `Blob`
* Parameters: `xpath` ("file:content" by default) and `entryName` (exact full path in the zip)
* Returns the corresponding file. Return null if the entry does not exist or is a folder


## Files > `ZipUtils.UnzipToDocumentsOp`
* Input is `Document` or `Blob`
* Extracts an archive and imports the files as Documents, creating the same structure.
* _Note that in all cases the operation creates a root Document at the `target`, it doesn't unzip to the target._
* When `input` is a Blob, the `target` parameter is required. When `input` is a Document the `target` is the parent of `input`.
* The `name` and `title` of the root document is the name of the archive file or the name of the root folder in the archive, by default. You can specify your own name with the `mainFolderishName` parameter.
* With regards to `mapRoot`: sometimes a zip file contains a single root folder and, thus, you want the root Document to be this folder - use `mapRoot = true` in this case. Other times the root Document is just a container to contain all the extracted content - use `mapRoot = false` in this case.
* Parameters:
  * `xpath` (optional): if `input` is a Document, this is the field that contains the archive
  * `target` (required if input is a blob): The parent Document where the import root is created
  * `folderishType` (optional): Type to use when creating Folderish children, default is `Folder`
  * `commitModulo` (optional): Save and commit transaction regularly incrementally (strongly recommended to avoid transaction timeout when you know the zip contains a lot of files), default `100`
  * `mainFolderishType` (optional): Type of the root Document, default is `Folder`
  * `mainFolderishName` (optional): The name for this main container
  * `mapRoot` (optional): Map the root folder of the archive to the root Document, or not. Default `false`.
* Returns the created root Folderish Document.


## Files > `ZipUtils.ZipFolderishOp`
* Input is a Folderish document
* Zip all the content recursively, with the hierarchy. Ignore non-folderish documents that have no blobs
* Returns the Zipped content
* Parameters:
  * `callbackChain` (optional): Byt default, the blob is read in "file:content". Use a callback chain to tune this behavior. Your chain _must_  receive `Document` as input and must output a Blob (even if null)
  * `whereClauseOverride`: To find the children of folderish documents, the operation exludes by default the children that are HiddenInNavigation, version, proxy, or in the trash. You can define your own filter. WARNING: do not start it with "AND", the code prefixes it for you.
    The default is `ecm:mixinType != 'HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:isTrashed = 0`
  * `doNotCreateMainFolder` (optionl): When `true` the zip archive TOC will not start with the name of the main folder.


## Files > `ZipUtils.ZipInfo`
* Input is `Document` or `Blob`
* Returns the input unchanged
* Parameter: `xpath`, optional ("file:content" by default)
* Return info about the zip in Context Variables: `zipInfo_comment`, `zipInfo_countFiles` (int), `zipInfo_countDirectories` (int)


# Build and Install

Build with maven (at least 3.3)

```
cd /path/to/nuxeo-zip-utils
mvn clean install
# _> the package is in nuxeo-zip-utils-package/target
```

# Support

**These features are not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


# Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


# About Nuxeo

Nuxeo, developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia. Learn more at [www.nuxeo.com](http://www.nuxeo.com).
