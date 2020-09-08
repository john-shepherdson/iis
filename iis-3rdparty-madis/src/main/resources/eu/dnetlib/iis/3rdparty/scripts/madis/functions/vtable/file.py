"""
.. function:: file(location, [formatting options])

Opens and returns a file or url as a table. The file's format is defined through
named options. *location* is defined through a URL, or a regular filename, can be given also as
the named parameter *url* or *file*. If no named parameters are given the returned table has one column
with each line of resource as a row or it assumes the dialect from the file ending (Files ending in .tsv, .csv, .json are
assumed to be in the corresponding dialect).

:Returned table schema:
    Columns are automatically named as *C1, C2...* or if header is set, columns are named by the resource first line value, and have the type *text*

Formatting options:

:fast:

    Default is 0 (false). Fast option speeds up the parsing of lines into values, exchanging accuracy for speed. It uses the delimiter option to split lines.

:strict:

    - strict:1  (default), if a failure occurs, the current transaction will be cancelled and an error will be returned.
    - strict:0  , returns all data that succesfully parses.
    - strict:-1 , returns all input lines in which the parser finds a problem. In essence this works as a negative parser.

    If no strict option is defined in fast:1 mode, then no strictness checking is applied at all, and an "Unknown error" will be returned if a problem occurs.

:encoding:

    A standar encoding name. (`List of encodings <http://docs.python.org/library/codecs.html#standard-encodings>`_)

:compression: *t/f*

    Default is *f* (False)

:compressiontype: *zip/gzip*

    Default is *zip*

Formatting options for CSV file types:

:dialect: *tsv/csv/json*

    Formats field as tab/comma separated values with minimal quoting. *JSON* dialect uses a line oriented *JSON* based format.

    File extensions that are recognised as a dialect (.tsv, .csv, .json) take precedence over a specified dialect parameter.

:header: *t/f*

    Set the column names of the returned table

:delimiter:

    A string used to separate fields. It defaults to ','

:doublequote: *t/f*

    Controls how instances of quotechar appearing inside a field should be themselves be quoted. When True, the character is doubled. When False, the escapechar is used as a prefix to the quotechar. It defaults to True.
    On output, if doublequote is False and no escapechar is set, Error is raised if a quotechar is found in a field

:escapechar:

    A one-character string used by the writer to escape the delimiter if quoting is set to QUOTE_NONE and the quotechar if doublequote is False. On reading, the escapechar removes any special meaning from the following character. It defaults to None, which disables escaping

:lineterminator:

    The string used to terminate lines produced by the writer. It defaults to '\\\\r\\\\n'

:quotechar:

    A one-character string used to quote fields containing special characters, such as the delimiter or quotechar, or which contain new-line characters. It defaults to '"'.

:quoting:

    Controls when quotes should be generated by the writer and recognised by the reader. It can take on any of the QUOTE_* constants and defaults to QUOTE_MINIMAL.
    Possible values are QUOTE_ALL, QUOTE_NONE, QUOTE_MINIMAL, QUOTE_NONNUMERIC

:skipinitialspace: *t/f*

    When True, whitespace immediately following the delimiter is ignored. The default is False

:toj: *Num*

    When toj is defined, columns 0-Num are returned as normal, and all columns >Num are returned as a JSON list or JSON
    dict, depending on if the *header* is enabled.

Examples::
  
    >>> sql("select * from (file file:testing/colpref.csv dialect:csv) limit 3;")
    C1     | C2    | C3         | C4
    --------------------------------------
    userid | colid | preference | usertype
    agr    |       | 6617580.0  | agr
    agr    | a0037 | 2659050.0  | agr
    >>> sql("select * from (file file:testing/colpref.csv dialect:csv header:t) limit 3")
    userid | colid | preference | usertype
    --------------------------------------
    agr    |       | 6617580.0  | agr
    agr    | a0037 | 2659050.0  | agr
    agr    | a0086 | 634130.0   | agr
    >>> sql("select * from (file file:testing/colpref.zip header:t dialect:csv compression:t) limit 3;")
    userid | colid | preference | usertype
    --------------------------------------
    agr    |       | 6617580.0  | agr
    agr    | a0037 | 2659050.0  | agr
    agr    | a0086 | 634130.0   | agr
    >>> sql("select * from (file 'testing/colpref.tsv' delimiter:| ) limit 3;")
    C1  | C2    | C3        | C4
    -----------------------------
    agr |       | 6617580.0 | agr
    agr | a0037 | 2659050.0 | agr
    agr | a0086 | 634130.0  | agr
    >>> sql("select * from (file 'testing/colpref.tsv.gz' delimiter:| compression:t compressiontype:gzip) limit 3;")
    C1  | C2    | C3        | C4
    -----------------------------
    agr |       | 6617580.0 | agr
    agr | a0037 | 2659050.0 | agr
    agr | a0086 | 634130.0  | agr
    >>> sql("select * from file('http://sites.google.com/site/stats202/data/test_data.csv?attredirects=0') limit 10;")
    C1
    -----------------
    Age,Number,Start
    middle,5,10
    young,2,17
    old,10,6
    young,2,17
    old,4,15
    middle,5,15
    young,3,13
    old,5,8
    young,7,9
    >>> sql("select * from file('file:testing/GeoIPCountryCSV.zip','compression:t','dialect:csv') limit 4")
    C1          | C2           | C3       | C4       | C5 | C6
    ----------------------------------------------------------------------
    2.6.190.56  | 2.6.190.63   | 33996344 | 33996351 | GB | United Kingdom
    3.0.0.0     | 4.17.135.31  | 50331648 | 68257567 | US | United States
    4.17.135.32 | 4.17.135.63  | 68257568 | 68257599 | CA | Canada
    4.17.135.64 | 4.17.142.255 | 68257600 | 68259583 | US | United States
"""

registered=True
external_stream=True

from vtiterable import SourceVT
from lib.dsv import reader                
import lib.gzip34 as gzip
import urllib2
import urlparse
import functions
from lib.iterutils import peekable
from lib.ziputils import ZipIter
import lib.inoutparsing
from functions.conf import domainExtraHeaders
from functions import mstr
import itertools
import json
import os.path
from codecs import utf_8_decode

csvkeywordparams=set(['delimiter','doublequote','escapechar','lineterminator','quotechar','quoting','skipinitialspace','dialect', 'fast'])

def nullify(iterlist):
    for lst in iterlist:
        yield [x if x.upper()!='NULL' else None for x in lst]

def directfile(f, encoding='utf_8'):
    for line in f:
        yield ( unicode(line.rstrip("\r\n"), encoding), )

def directfileutf8(f):
    try:
        for line in f:
            yield ( utf_8_decode(line.rstrip("\r\n"))[0], )
    except UnicodeDecodeError, e:
        raise functions.OperatorError(__name__.rsplit('.')[-1], unicode(e)+"\nFile is not %s encoded" %(self.encoding))

def strict0(tabiter, colcount):
    while True:
        row = tabiter.next()
        if len(row) == colcount:
            yield row

def convnumbers(r):
    out = []
    for c in r:
        try:
            c = int(c)
        except ValueError:
            try:
                c = float(c)
            except ValueError:
                pass
        out.append(c)
    return out

def tojdict(tabiter, header, preable):
    for r in tabiter:
        yield r[:preable] + [json.dumps(dict(zip(header, convnumbers(r[preable:]))), separators=(',',':'), ensure_ascii=False)]

def tojlist(tabiter, preable):
    for r in tabiter:
        yield r[:preable] + [json.dumps(convnumbers(r[preable:]), separators=(',',':'), ensure_ascii=False)]

def strict1(tabiter, colcount):
    linenum = 0
    while True:
        row = tabiter.next()
        linenum += 1
        if len(row) != colcount:
            raise functions.OperatorError(__name__.rsplit('.')[-1],"Line " + str(linenum) + " is invalid. Found "+str(len(row))+" of expected "+str(colcount)+" columns\n"+"The line's parsed contents are:\n" + u','.join([mstr(x) for x in row]))
        yield row

def strictminus1(tabiter, colcount, hasheader = False):
    linenum = 0
    if hasheader:
        linenum += 1
    while True:
        linenum += 1
        row = tabiter.next()
        if len(row) != colcount:
            yield (linenum, len(row), colcount, u','.join([unicode(x) for x in row]))

def cleanBOM(t):
    return t.encode('ascii', errors = 'ignore').strip()

class FileCursor:
    def __init__(self,filename,isurl,compressiontype,compression,hasheader,first,namelist,extraurlheaders,**rest):
        self.encoding='utf_8'
        self.fast = False
        self.strict = None
        self.toj = -1
        self.namelist = None
        self.hasheader = hasheader
        self.namelist = namelist

        if 'encoding' in rest:
            self.encoding=rest['encoding']
            del rest['encoding']

        if 'strict' in rest:
            self.strict = int(rest['strict'])
            del rest['strict']

        if 'fast' in rest:
            self.fast = True
            del rest['fast']

        if 'toj' in rest:
            try:
                self.toj = int(rest['toj'])
            except ValueError:
                self.toj = 0
            del rest['toj']

        self.nonames=first
        for el in rest:
            if el not in csvkeywordparams:
                raise functions.OperatorError(__name__.rsplit('.')[-1],"Invalid parameter %s" %(el))

        pathname=None
        gzipcompressed=False
        
        try:
            if compression and compressiontype=='zip':
                self.fileiter=ZipIter(filename,"r")
            elif not isurl:
                pathname=filename.strip()
                if self.fast or compression or (pathname!=None and ( pathname.endswith('.gz') or pathname.endswith('.gzip') )):
                    self.fileiter=open(filename,"r", buffering=1000000)
                else:
                    if "MSPW" in functions.apsw_version:
                        self.fileiter=open(filename,"r", buffering=1000000)
                    else:
                        self.fileiter=open(filename,"rU", buffering=1000000)
            else:
                pathname=urlparse.urlparse(filename)[2]
                req=urllib2.Request(filename,None,extraurlheaders)
                hreq=urllib2.urlopen(req)
                if [1 for x,y in hreq.headers.items() if x.lower() in ('content-encoding', 'content-type') and y.lower().find('gzip')!=-1]:
                    gzipcompressed=True
                self.fileiter=hreq

            if pathname!=None and ( pathname.endswith('.gz') or pathname.endswith('.gzip') ):
                gzipcompressed=True

            if compression and compressiontype=='gz':
                gzipcompressed=True

            if gzipcompressed:
                if filename.endswith('.gz'):
                    filename = filename[:-3]
                if filename.endswith('.gzip'):
                    filename = filename[:-5]
                self.fileiter = gzip.GzipFile(mode = 'rb', fileobj=self.fileiter)

        except Exception,e:
            raise functions.OperatorError(__name__.rsplit('.')[-1],e)

        _, filenameExt = os.path.splitext(filename)
        filenameExt = filenameExt.lower()

        if filenameExt == '.json' or filenameExt == '.js' or ('dialect' in rest and type(rest['dialect']) == str and rest['dialect'].lower()=='json'):
            self.fast = True
            firstline = self.fileiter.readline()
            schemaline = json.loads(firstline)
            schemalinetype = type(schemaline)

            if schemalinetype == list:
                for i in xrange(1, len(schemaline)+1):
                    namelist.append( ['C'+str(i), 'text'] )
                self.fileiter = itertools.chain([firstline], self.fileiter)

            elif schemalinetype == dict:
                namelist += schemaline['schema']

            else:
                raise functions.OperatorError(__name__.rsplit('.')[-1], "Input file is not in line JSON format")

            if "MSPW" in functions.apsw_version:
                self.iter = (json.loads(x) for x in self.fileiter)
            else:
                jsonload = json.JSONDecoder().scan_once
                self.iter = (jsonload(x, 0)[0] for x in self.fileiter)
            return

        if filenameExt =='.csv':
            if self.fast:
                rest['delimiter'] = ','
            rest['dialect']=lib.inoutparsing.defaultcsv()

        if filenameExt == '.tsv':
            if self.fast:
                rest['delimiter'] = '\t'
            rest['dialect']=lib.inoutparsing.tsv()

        if hasheader or len(rest)>0: #if at least one csv argument default dialect is csv else line
            if 'dialect' not in rest:
                rest['dialect']=lib.inoutparsing.defaultcsv()

            linelen = 0
            if first and not hasheader:
                if self.fast:
                    rest['fast'] = True
                    self.iter=peekable(reader(self.fileiter,encoding=self.encoding, **rest))
                else:
                    self.iter=peekable(nullify(reader(self.fileiter,encoding=self.encoding,**rest)))
                    if self.strict == None:
                        self.strict = 1
                sample=self.iter.peek()
                linelen = len(sample)
            else: ###not first or header
                if self.fast:
                    rest['fast'] = True
                    self.iter=iter(reader(self.fileiter,encoding=self.encoding,**rest))
                else:
                    self.iter=nullify(reader(self.fileiter, encoding=self.encoding, **rest))
                    if self.strict == None:
                        self.strict = 1
                linelen = len(namelist)

                if hasheader:
                    sample=self.iter.next()
                    linelen = len(sample)

            if self.strict == 0:
                self.iter = strict0(self.iter, linelen)

            if self.strict == 1:
                self.iter = strict1(self.iter, linelen)

            if self.strict == -1:
                self.iter = strictminus1(self.iter, linelen, hasheader)
                namelist += [['linenumber', 'int'], ['foundcols', 'int'], ['expectedcols', 'int'],['contents', 'text']]

            if first and namelist==[]:
                if hasheader:
                    for i in sample:
                        namelist.append( [cleanBOM(i), 'text'] )
                else:
                    for i in xrange(1, linelen+1):
                        namelist.append( ['C'+str(i), 'text'] )

        else: #### Default read lines
            if self.encoding == 'utf_8':
                self.iter = directfileutf8(self.fileiter)
                self.fast = True
            else:
                self.iter = directfile(self.fileiter, encoding=self.encoding)
            namelist.append( ['C1', 'text'] )

        if self.toj >=0:
            header = [x[0] for x in namelist]
            while len(namelist) > self.toj: namelist.pop()
            header = header[self.toj:]
            if self.hasheader:
                namelist.append( ['Cjdict', 'text'] )
                self.iter = tojdict(self.iter, header, self.toj)
            else:
                namelist.append( ['Cjlist', 'text'] )
                self.iter = tojlist(self.iter, self.toj)

        if self.fast:
            self.next = self.iter.next

    def __iter__(self):
        if self.fast:
            return self.iter
        else:
            return self

    def next(self):
        try:
            return self.iter.next()
        except UnicodeDecodeError, e:
            raise functions.OperatorError(__name__.rsplit('.')[-1], unicode(e)+"\nFile is not UTF8 encoded")

    def close(self):
        self.fileiter.close()
        
class FileVT:
    def __init__(self,envdict,largs,dictargs): #DO NOT DO ANYTHING HEAVY
        self.largs=largs
        self.envdict=envdict
        self.dictargs=dictargs
        self.nonames=True
        self.names=[]
        self.destroyfiles=[]
        self.inoutargs={}
        self.extraheader={}
        
    def getdescription(self):
        if not self.names:
            raise functions.OperatorError(__name__.rsplit('.')[-1],"VTable getdescription called before initiliazation")
        self.nonames=False
        return self.names
    
    def open(self):
        if self.nonames:
            try:
                self.inoutargs=lib.inoutparsing.inoutargsparse(self.largs,self.dictargs)
            except lib.inoutparsing.InputsError:
                raise functions.OperatorError(__name__.rsplit('.')[-1]," One source input is required")
            if not self.inoutargs['filename']:
                raise functions.OperatorError(__name__.rsplit('.')[-1],"No input provided")
            
            if self.inoutargs['url']:
                for domain in domainExtraHeaders:
                    if domain in self.inoutargs['filename']:
                        self.extraheader=domainExtraHeaders[domain]
                        break
                if 'User-Agent' not in self.extraheader:
                    self.extraheader['User-Agent']='Mozilla/4.0 (compatible; MSIE 5.5; Windows NT)'
            if self.inoutargs['url'] and self.inoutargs['compression'] and self.inoutargs['compressiontype']=='zip':
                self.inoutargs['filename']=lib.inoutparsing.cacheurl(self.inoutargs['filename'], self.extraheader)
                self.destroyfiles=[self.inoutargs['filename']]
                self.inoutargs['url']=False
        
        return FileCursor(self.inoutargs['filename'],self.inoutargs['url'],self.inoutargs['compressiontype'],self.inoutargs['compression'],self.inoutargs['header'],self.nonames,self.names,self.extraheader,**self.dictargs)

    def destroy(self):
        import os
        for f in self.destroyfiles:
            os.remove(f)

def Source():
    global boolargs, nonstringargs
    return SourceVT(FileVT, lib.inoutparsing.boolargs+['header','compression'], lib.inoutparsing.nonstringargs, lib.inoutparsing.needsescape)


if not ('.' in __name__):
    """
    This is needed to be able to test the function, put it at the end of every
    new function you create
    """
    import sys
    import setpath
    from functions import *
    testfunction()
    if __name__ == "__main__":
        reload(sys)
        sys.setdefaultencoding('utf-8')
        import doctest
        doctest.testmod()
