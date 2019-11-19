package carnival.util


import java.text.SimpleDateFormat

import groovy.mock.interceptor.StubFor
import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper



/**
 * gradle test --tests "carnival.core.util.MappedDataTableSpec"
 *
 */
class MappedDataTableSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    @Shared testDate


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        testDate = new Date("01/01/2000")
    } 


    def cleanupSpec() {
    }




    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////

    java.sql.ResultSet mockResultSet(Map m) {
        def entries = m.entrySet().toList()

        def getColumnCount = {
            return entries.size()
        }
        def getColumnLabel = { i ->
            if (i > entries.size()) throw new RuntimeException("$i")
            else return entries[i-1].getKey()
        }

        def getObject = { Object key ->
            m.get(key)
        }

        [
            getMetaData: {[
                getColumnCount: getColumnCount,
                getColumnLabel: getColumnLabel
            ] as java.sql.ResultSetMetaData },
            getObject: getObject
        ] as java.sql.ResultSet
    }




    ///////////////////////////////////////////////////////////////////////////
    // KEY TYPE
    ///////////////////////////////////////////////////////////////////////////

    def "default idKeyType"() {
        given:
        def mdt
        Throwable e

        when:        
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id')

        then:
        mdt.idKeyType == KeyType.GENERIC_STRING_ID

        when:
        mdt.dataAddWithModifications(id:'id1', v1:'v11')

        then:
        mdt.dataGet('id1').size() == 2
        mdt.dataGet('id1').get(DataTable.toFieldName('id')) == 'id1'
        mdt.dataGet('id1').get(DataTable.toFieldName('v1')) == 'v11'
    }


    ///////////////////////////////////////////////////////////////////////////
    // MAPPEDDATAINTERFACE - MAPPED DATA INTERFACE
    ///////////////////////////////////////////////////////////////////////////


    def "dataAdd ResultSet dataFieldPrefix"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = [
            getMetaData: { [
                getColumnCount: { 
                    return 2 
                },
                getColumnLabel: { i -> 
                    switch (i) {
                        case 1: return 'id';
                        case 2: return 'd1';
                        default: throw new RuntimeException("$i") 
                    } 
                }
            ] as java.sql.ResultSetMetaData },
            getObject: { Object key ->
                switch (String.valueOf(key)) {
                    case 'id': return 'id1';
                    case 'd1': return 'd11';
                    default: throw new RuntimeException("$key") 
                } 
            }
        ] as java.sql.ResultSet
        mdt.dataAdd(res, 'id' , 'pre_')

        then:
        mdt.data['id1'].size() == 2
        mdt.dataGet('id1').get(DataTable.toFieldName('id')) == 'id1'
        mdt.dataGet('id1').get(DataTable.toFieldName('pre_d1')) == 'd11'

        when:
        res = mockResultSet(id:'id1', d1:'d11')
        mdt.dataAdd(res, 'id' , 'pre_')

        then:
        e = thrown()

        when:
        res = mockResultSet(id_:'id2', d1:'d12')
        mdt.dataAdd(res, 'id' , 'pre_')

        then:
        e = thrown()

        when:
        res = mockResultSet(ID:'id2', d1:'d12')
        mdt.dataAdd(res, 'id' , 'pre_')

        then:
        mdt.data['id2'].size() == 2
    }



    def "dataAddWithModifications dataFieldPrefix"() {
        given:
        def mdt
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        mdt.dataAddWithModifications([id:'id1', v1:'v11'], [dataFieldPrefix:'pre_'])

        then:
        mdt.dataGet('id1').size() == 2
        mdt.dataGet('id1').get(DataTable.toFieldName('id')) == 'id1'
        mdt.dataGet('id1').get(DataTable.toFieldName('pre_v1')) == 'v11'

        // cannot add id1 twice
        when:
        mdt.dataAddWithModifications([id:'id1', v2:'v21'], [dataFieldPrefix:'pre_'])

        then:
        e = thrown()

        // does not contain key 'id'
        when:
        mdt.dataAddWithModifications([id_:'id1', v1:'v11'], [dataFieldPrefix:'pre_'])

        then:
        e = thrown()

        // multiple id key matches
        when:
        mdt.dataAddWithModifications([id:'id1', ID:'id1'], [dataFieldPrefix:'pre_'])

        then:
        e = thrown()

        when:
        mdt.dataAddWithModifications([ID:'id2', v1:'v12'], [dataFieldPrefix:'pre_'])

        then:
        mdt.dataGet('id2').size() == 2
    }




    ///////////////////////////////////////////////////////////////////////////
    // DATA APPEND
    ///////////////////////////////////////////////////////////////////////////

    def "dataAppend(id,map) data cannot contain id field"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData
        Throwable e

        when:
        mdt.dataAppend('1', [ID:'1', v1:'v11'])

        then:
        e = thrown()
        e instanceof IllegalArgumentException
        //e.printStackTrace()
    }


    def "dataAppend(id,map) cannot add conflicting"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData
        Throwable e

        when:
        mdt.dataAppend('1', [v1:'v11'])
        mdt.dataAppend('1', [v1:'v11', v2:'v21'])

        then:
        mdt.data.size() == 1

        when:
        mdt.dataAppend('1', [v1:'v11_'])

        then:
        e = thrown()
        e instanceof IllegalArgumentException
        //e.printStackTrace()
    }


    def "dataAppend(id,map) uses toIdValue"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('A', [v1:'v11'])

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('a')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == 'a'
        mdtData.get('V1') == 'v11'
    }


    def "dataAppend(id,map) secondary id field"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI, secondaryIdFieldMap:[ID2:KeyType.MRN])
        def mdtData

        when:
        mdt.dataAppend('1', [ID2:'ID2VAL', v1:'v11'])

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 3
        mdtData.get('ID') == '1'
        mdtData.get('ID2') == 'id2val'
        mdtData.get('V1') == 'v11'

        when:
        mdt.dataAppend('2', [ID2:null, v1:'v11'])

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 2

        when:
        mdtData = mdt.dataGet('2')

        then:
        mdtData.size() == 3
        mdtData.get('ID') == '2'
        mdtData.get('ID2') == null
        mdtData.get('V1') == 'v11'

    }

        
    def "dataAppend(id,map) field name formatting"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('1', [v1:'v11'])

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
    }


    def "dataAppend(id,map)"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('1', [v1:'v11'])

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
    }


    def "dataAppend individual vals uses toIdValue"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('A', 'v1', 'v11')

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('a')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == 'a'
        mdtData.get('V1') == 'v11'
    }

    def "dataAppend individual vals secondary id field"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI, secondaryIdFieldMap:[ID2:KeyType.MRN])
        def mdtData

        when:
        mdt.dataAppend('1', 'v1', 'v11')

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'

        when:
        mdt.dataAppend('1', 'v2', 'v21')

        then:
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 3
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
        mdtData.get('V2') == 'v21'

        when:
        mdt.dataAppend('1', 'ID2', 'ID2VAL')

        then:
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 4
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
        mdtData.get('V2') == 'v21'
        mdtData.get('ID2') == 'id2val'

        /*
        // currently not allowing null values
        when:
        mdt.dataAppend('2', 'ID2', null)

        then:
        mdt.data.size() == 2

        when:
        mdtData = mdt.dataGet('2')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('ID2') == null
        */
    }

    
    // #70
    def "dataAppend individual vals field name formatting"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('1', 'v1', 'v11')

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'

        when:
        mdt.dataAppend('1', 'v2', 'v21')

        then:
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 3
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
        mdtData.get('V2') == 'v21'
    }


    def "dataAppend individual vals"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'ID', idKeyType:KeyType.EMPI)
        def mdtData

        when:
        mdt.dataAppend('1', 'v1', 'v11')

        then:
        mdt.keySet.contains('ID')
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 2
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'

        when:
        mdt.dataAppend('1', 'v2', 'v21')

        then:
        mdt.data.size() == 1

        when:
        mdtData = mdt.dataGet('1')

        then:
        mdtData.size() == 3
        mdtData.get('ID') == '1'
        mdtData.get('V1') == 'v11'
        mdtData.get('V2') == 'v21'
    }



    ///////////////////////////////////////////////////////////////////////////
    // NULL HANDLING
    ///////////////////////////////////////////////////////////////////////////

    def "dataAdd ResultSet nulls"() {
        given:
        def mdt
        def res
        def rec
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = mockResultSet(id:'id1', d1:null)

        then:
        res.getMetaData().getColumnCount() == 2
        res.getMetaData().getColumnLabel(1) == 'id'
        res.getMetaData().getColumnLabel(2) == 'd1'
        res.getObject('id') == 'id1'
        res.getObject('d1') == null

        when:
        mdt.dataAdd(res)
        rec = mdt.dataGet('id1')
        println "rec: $rec"

        then:
        rec != null
        rec.get('D1') == null
    }


    def "dataAdd GroovyRowResult nulls"() {
        given:
        def mdt
        def res
        def rec
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = [id:'id1', d1:null] as GroovyRowResult

        then:
        res.keySet().size() == 2
        res.get('id') == 'id1'
        res.get('d1') == null

        when:
        mdt.dataAdd(res)
        rec = mdt.dataGet('id1')

        then:
        rec.size() == 2
        rec.get('D1') == null
    }



    def "createFromFiles empty strings are maintained"() {
        given:
        def mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def rec
        def file
        def buildDir = new File('build')

        when:
        mdt.dataAdd(id:'id1', v1:'v11')
        mdt.dataAdd(id:'id2', v1:'')
        file = mdt.writeFiles(buildDir)
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test')
        def data = mdtFromFile.data

        then:
        data.size() == 2

        when:
        rec = mdtFromFile.dataGet('id1')

        then:
        rec.ID == 'id1'
        rec.V1 == 'v11'

        when:
        rec = mdtFromFile.dataGet('id2')

        then:
        rec.keySet().size() == 2
        rec.containsKey('ID')
        rec.containsKey('V1')
        rec.ID == 'id2'
        rec.V1 != null
        rec.V1 == ''
    }


    def "createFromFiles canonical nulls are explicitly mapped to null"() {
        given:
        def mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def rec
        def file
        def buildDir = new File('build')

        when:
        mdt.dataAdd(id:'id1', v1:'v11')
        mdt.dataAdd(id:'id2', v1:null)
        file = mdt.writeFiles(buildDir)
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test')
        def data = mdtFromFile.data

        then:
        data.size() == 2

        when:
        rec = mdtFromFile.dataGet('id1')

        then:
        rec.ID == 'id1'
        rec.V1 == 'v11'

        when:
        rec = mdtFromFile.dataGet('id2')

        then:
        rec.keySet().size() == 2
        rec.containsKey('ID')
        rec.containsKey('V1')
        rec.ID == 'id2'
        rec.V1 == null
    }


    def "createFromFiles missing values are not added to the row map"() {
        given:
        def mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def rec
        def file
        def buildDir = new File('build')

        when:
        mdt.dataAdd(id:'id1', v1:'v11')
        mdt.dataAdd(id:'id2')
        file = mdt.writeFiles(buildDir)
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test')
        def data = mdtFromFile.data

        then:
        data.size() == 2

        when:
        rec = mdtFromFile.dataGet('id1')

        then:
        rec.ID == 'id1'
        rec.V1 == 'v11'

        when:
        rec = mdtFromFile.dataGet('id2')

        then:
        rec.keySet().size() == 1
        rec.containsKey('ID')
        !rec.containsKey('V1')
        rec.ID == 'id2'
    }


    def "writeDataFile write missing values as canonical string"() {
        given:
        def mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def d
        def file

        when:
        mdt.dataAdd(id:'id1', v1:'v11')
        mdt.dataAdd(id:'id2')
        file = mdt.writeDataFile(new File('build'))

        then:
        file.exists()
        file.name.endsWith('.csv')
        file.canonicalPath.indexOf('build') >= 0

        when:
        def fdata = DataTable.readDataFromCsvFile(file)
        def rec = fdata[0]
        println "rec: $rec"

        then:
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        rec = fdata[1]

        then:
        rec['ID'] == 'id2'
        rec['V1'] == DataTable.MISSING
    }


    def "writeDataFile write null values as canonical string"() {
        given:
        def mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def d
        def file

        when:
        mdt.dataAdd(id:'id1', v1:null)
        file = mdt.writeDataFile(new File('build'))

        then:
        file.exists()
        file.name.endsWith('.csv')
        file.canonicalPath.indexOf('build') >= 0

        when:
        def fdata = DataTable.readDataFromCsvFile(file)
        def rec = fdata[0]
        println "rec: $rec"

        then:
        rec['ID'] == 'id1'
        rec['V1'] == DataTable.NULL
    }


    def "dataAdd do not map nulls to a string"() {
        given:
        def res
        def d
        def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        mdt.dataAdd(id:'id1', v1:null)
        d = mdt.dataGet('id1')

        then:
        d.ID == 'id1'
        d.V1 == null
    }



    def "test map null"() {
        when:
        def m = [:]
        def x = m["x"]

        then:
        x == null
        !m.containsKey("x")

        when:
        m["y"] = null

        then:
        m["y"] == null
        m.containsKey("y")
    }



    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    def "constructor"() {
        given:
        def mdt
        def data
        def args
        Throwable e
        def now = new Date()

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI)

        then:
        mdt != null
        mdt.name == "some-name"
        mdt.idFieldName == DataTable.toFieldName("id-key")
        mdt.idKeyType == KeyType.EMPI
        mdt.queryDate.getTime() - now.getTime() <= 1000
        mdt.dataSourceDateOfUpdate == null

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI, secondaryIdFieldMap:["key2":KeyType.MRN, "key3":KeyType.PK_PATIENT_ID])

        then:
        mdt != null
        mdt.name == "some-name"
        mdt.idFieldName == DataTable.toFieldName("id-key")
        mdt.idKeyType == KeyType.EMPI
        mdt.secondaryIdFieldMap.equals([KEY2:KeyType.MRN, KEY3:KeyType.PK_PATIENT_ID])
        mdt.queryDate.getTime() - now.getTime() <= 1000

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI, secondaryIdFieldMap:["id-key":KeyType.MRN, "key3":KeyType.PK_PATIENT_ID])

        then:
        e = thrown()

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI, queryDate:testDate)

        then:
        mdt != null
        mdt.name == "some-name"
        mdt.idFieldName == DataTable.toFieldName("id-key")
        mdt.idKeyType == KeyType.EMPI
        mdt.queryDate.equals(testDate)

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI, queryDate:DataTable.dataSetDateToString(testDate))

        then:
        mdt != null
        mdt.name == "some-name"
        mdt.idFieldName == DataTable.toFieldName("id-key")
        mdt.idKeyType == KeyType.EMPI
        mdt.queryDate.equals(testDate)

        when:
        mdt = new MappedDataTable(name:"some-name", idFieldName:"id-key", idKeyType:KeyType.EMPI, queryDate:"bad dates")

        then:
        e = thrown()
    }


    ///////////////////////////////////////////////////////////////////////////
    // CSV FILES
    ///////////////////////////////////////////////////////////////////////////

    def "create from empty data file"() {
        given:
        def edf = new File('build/md-test.csv')
        def emf = new File('build/md-test.yaml')
        def buildDir = new File('build')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()

        when:
        def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'mdt-test.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'mdt-test.yaml'
        //mf.length() > 0

        when:
        def dfText = df.text
        if (dfText) dfText = dfText.trim()

        then:
        !dfText

        when:
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test')

        then:
        mdtFromFile
        mdtFromFile.data.size() == 0        
    }


    def "gStringImpl test"() {
        when: 
        def testString1 = "foo1"
        def testGString = "g-${testString1}"


        java.lang.Object[] values = ['foo1']
        java.lang.String[] strings = ['g-', '']
        def constGStrImpl = new org.codehaus.groovy.runtime.GStringImpl(values, strings)

        then:
        constGStrImpl.equals(testGString)
        testGString.toString().equals("g-foo1")

    } 

    def "create from empty data file with secondary ids"() {
        given:
        def edf = new File('build/md-test.csv')
        def emf = new File('build/md-test.yaml')
        def buildDir = new File('build')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()

        when:
        def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI, secondaryIdFieldMap:["key2":KeyType.MRN, "key3":KeyType.PK_PATIENT_ID])
        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'mdt-test.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'mdt-test.yaml'
        //mf.length() > 0

        when:
        def dfText = df.text
        if (dfText) dfText = dfText.trim()

        then:
        !dfText

        when:
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test')

        then:
        mdtFromFile
        mdtFromFile.data.size() == 0
        mdtFromFile.name == "mdt-test"
        mdtFromFile.idFieldName == DataTable.toFieldName("id")
        mdtFromFile.idKeyType == KeyType.EMPI
        mdtFromFile.secondaryIdFieldMap.equals([KEY2:KeyType.MRN, KEY3:KeyType.PK_PATIENT_ID])
    }


    def "create from empty data file with vine args"() {
        given:
        def edf = new File('build/md-test-vine.csv')
        def emf = new File('build/md-test-vine.yaml')
        def buildDir = new File('build')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()

        when:
        def icdGroup = CodeRefGroup.create(name:'A', icd:['1.1', '1.2', '2.1', '2.11'])
        def testString1 = "foo1"
        def testString2 = "foo2"
        def vine = [
            name:"vineName",
            method:"vineMethod",
            args:[
                int1:1,
                string1:"testString", 
                gString1:"g-${testString1}",
                stringList:["test1", "test2", "test3"],
                gStringList:["g-${testString1}", "g-${testString2}"],
                stringSet:["testSet1", "testSet2", "testSet3"] as Set,
                icdGroup:icdGroup, 
                dateComp:SingleValueDateComparator.ALL_MOSTRECENT
            ]
        ]
        // should be equivalent to vine
        def icdGroupReorder = CodeRefGroup.create(name:'A', icd:['2.11', '1.1', '1.2', '2.1'])
        def vineReorder = [
            name:"vineName",
            method:"vineMethod",
            args:[
                int1:1,
                string1:"testString", 
                gString1:"g-foo1",
                stringList:["test1", "test2", "test3"],
                gStringList:["g-${testString1}", "g-${testString2}"],
                stringSet:["testSet3", "testSet1", "testSet2"] as Set,
                icdGroup:icdGroupReorder, 
                dateComp:SingleValueDateComparator.ALL_MOSTRECENT
            ]
        ]

        //println "gStringList class:"
        //println vineReorder.args.gStringList.first().class
        //println vineReorder.args.gStringList.first() instanceof GString
        //println vineReorder.args.gStringList.first() instanceof GStringImpl



        def mdt = new MappedDataTable(name:'mdt-test-vine', idFieldName:'id', idKeyType:KeyType.EMPI, vine:vine)
        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'mdt-test-vine.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'mdt-test-vine.yaml'
        //mf.length() > 0

        when:
        def dfText = df.text
        if (dfText) dfText = dfText.trim()

        then:
        !dfText

        when:
        def mdtFromFile = MappedDataTable.createFromFiles(buildDir, 'mdt-test-vine')
        println "create from empty data file with vine args"
        println "vine:"
        println mdtFromFile.vine

        then:
        mdtFromFile
        mdtFromFile.data.size() == 0
        mdtFromFile.name == "mdt-test-vine"
        mdtFromFile.idFieldName == DataTable.toFieldName("id")
        mdtFromFile.idKeyType == KeyType.EMPI
        mdtFromFile.vine
        mdtFromFile.vine.name.equals(vine.name)
        mdtFromFile.vine.method.equals(vine.method)
        for(e in vine.args) {
            assert mdtFromFile.vine.args.get(e.key)
            assert mdtFromFile.vine.args.get(e.key).equals(e.value)
            println "${mdtFromFile.vine.args.get(e.key)} -> ${e.value}"
            //assert mdtFromFile.vine.args.get(e.key) instanceof e.value.class
        }
        for(e in vineReorder.args) {
            assert mdtFromFile.vine.args.get(e.key)
            assert mdtFromFile.vine.args.get(e.key) == e.value
            println "${mdtFromFile.vine.args.get(e.key)} -> ${e.value}"
            //assert mdtFromFile.vine.args.get(e.key) instanceof e.value.class
        }
    }


    def "createFromFile"() {
        given:
        def edf = new File('build/md-test.csv')
        def emf = new File('build/md-test.yaml')
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()
        def mdt0 = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI, queryDate:testDate)
        mdt0.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        def buildDir = new File('build')
        def res = mdt0.writeFiles(buildDir)

        when:
        def mdt = MappedDataTable.createFromFiles(buildDir, 'mdt-test')

        then:
        mdt != null
        mdt.name == 'mdt-test'
        mdt.queryDate == testDate
        mdt.idFieldName == DataTable.toFieldName('id')
        mdt.idKeyType == KeyType.EMPI

        when:
        def data = mdt.data

        then:
        data != null
        data.size() == 2

        when:
        def r1 = data['id1']

        then:
        r1['ID'] == 'id1'
        r1['V1'] == 'v11'

        when:
        def r2 = data['id2']

        then:
        r2['ID'] == 'id2'
        r2['V1'] == 'v12'
    }



    def "writeFiles"() {
        given:
        def edf = new File('build/md-test.csv')
        def emf = new File('build/md-test.yaml')
        def buildDir = new File('build')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()

        when:
        def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'mdt-test.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'mdt-test.yaml'
        mf.length() > 0
    }



    def "write meta file with vine args"() {
        given:
        def edf = new File('build/md-test-vine.csv')
        def emf = new File('build/md-test-vine.yaml')
        def buildDir = new File('build')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()

        when:
        def icdGroup = CodeRefGroup.create(name:'A', icd:['1.1', '1.2', '2.1', '2.11'])
        def testString1 = "foo1"
        def testString2 = "foo2"
        def qp = [class:'QueryProcess']
        def vine = [
            name:"vineName",
            method:"vineMethod",
            queryProcess:qp,
            args:[
                int1:1,
                string1:"testString", 
                gString1:"g-${testString1}",
                stringList:["test1", "test2", "test3"],
                gStringList:["g-${testString1}", "g-${testString2}"],
                stringSet:["testSet1", "testSet2", "testSet3"] as Set,
                icdGroup:icdGroup, 
                dateComp:SingleValueDateComparator.ALL_MOSTRECENT
            ]
        ]

        def mdt = new MappedDataTable(name:'mdt-test-vine', idFieldName:'id', idKeyType:KeyType.EMPI, vine:vine)
        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'mdt-test-vine.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'mdt-test-vine.yaml'
        //mf.length() > 0

        when:
        def dfText = df.text
        if (dfText) dfText = dfText.trim()

        then:
        !dfText

        when:
        def mfText = mf.text

        then:
        mfText
        mfText.contains('queryProcess')
    }


    def "writeMetaFile data"() {
        given:
        def mdt
        def data
        def file
        def meta
        def metaDataSourceDateOfUpdate
        Throwable e
        def now = new Date()
        def yesterday = now.plus(-1)
        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableRepresenter())

        when:
        mdt = new MappedDataTable(name:'mdt-test-file', idFieldName:'id', idKeyType:KeyType.EMPI)
        file = mdt.writeMetaFile(new File('build'))
        println "${file.name} ${file.canonicalPath}"

        then:
        file.exists()
        file.name == 'mdt-test-file.yaml'
        file.canonicalPath.indexOf('build') >= 0

        when:
        meta = yaml.load(file.text)

        then:
        meta.name == 'mdt-test-file'
        meta.idFieldName == DataTable.toFieldName('id')
        "${meta.idKeyType}" == "EMPI"
        meta.queryDate.getTime() - now.getTime() <= 1000

        when:
        mdt = new MappedDataTable(name:'mdt-test-file', idFieldName:'id', idKeyType:KeyType.EMPI, queryDate:testDate)
        mdt.dataSourceDateOfUpdate = yesterday
        file = mdt.writeMetaFile(new File('build'))
        println "${file.name} ${file.canonicalPath}"

        then:
        file.exists()
        file.name == 'mdt-test-file.yaml'
        file.canonicalPath.indexOf('build') >= 0

        when:
        meta = yaml.load(file.text)
        metaDataSourceDateOfUpdate = DataTable.dataDate(meta.dataSourceDateOfUpdate)

        then:
        meta.name == 'mdt-test-file'
        meta.queryDate == testDate
        meta.dataSourceDateOfUpdate == yesterday
        //metaDataSourceDateOfUpdate.getTime().equals(yesterday.getTime())
        metaDataSourceDateOfUpdate.getTime() - yesterday.getTime() <= 1000 // milliseconds truncated during yaml conversion
        meta.idFieldName == DataTable.toFieldName('id')
        "${meta.idKeyType}" == "EMPI"
    }


    def "writeMetaFile data with secondary ids"() {
        given:
        def mdt
        def data
        def file
        Throwable e
        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableRepresenter())

        when:
        mdt = new MappedDataTable(
            name:'mdt-test', 
            idFieldName:'id', 
            idKeyType:KeyType.EMPI, 
            secondaryIdFieldMap:[
                key2:KeyType.MRN, 
                key3:KeyType.PK_PATIENT_ID
            ]
        )
        file = mdt.writeMetaFile(new File('build'))
        println "${file.name} ${file.canonicalPath}"

        then:
        file.exists()
        file.name == 'mdt-test.yaml'
        file.canonicalPath.indexOf('build') >= 0

        when:
        def meta = yaml.load(file.text)

        then:
        meta.name == 'mdt-test'
        meta.idFieldName == DataTable.toFieldName('id')
        "${meta.idKeyType}" == "EMPI"
        meta.secondaryIdFieldMap
        meta.secondaryIdFieldMap.size() == 2
        meta.secondaryIdFieldMap.get('KEY2') == KeyType.MRN
        meta.secondaryIdFieldMap.get('KEY3') == KeyType.PK_PATIENT_ID

        meta.secondaryIdFieldMap.equals([KEY2:KeyType.MRN, KEY3:KeyType.PK_PATIENT_ID])
    }



    def "writeDataFile data"() {
        given:
        def mdt
        def data
        def file
        Throwable e

        when:
        mdt = new MappedDataTable(name: 'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        file = mdt.writeDataFile(new File('build'))

        then:
        file.exists()
        file.name.endsWith('.csv')
        file.canonicalPath.indexOf('build') >= 0

        when:
        def fdata = DataTable.readDataFromCsvFile(file)
        def rec = fdata[idx]
        println "rec: $rec"

        then:
        rec['ID'] == id
        rec['V1'] == v1

        where:
        idx << [0, 1]
        id << ['id1', 'id2']
        v1 << ['v11', 'v12']
    }


    def "writeDataToCsvFile file data"() {
        given:
        def mdt
        def data
        def file
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        file = mdt.writeDataToCsvFile(new File('build/mdt-test.csv'))

        then:
        file.exists()

        when:
        def fdata = DataTable.readDataFromCsvFile(file)
        def rec = fdata[idx]
        println "rec: $rec"

        then:
        rec['ID'] == id
        rec['V1'] == v1

        where:
        idx << [0, 1]
        id << ['id1', 'id2']
        v1 << ['v11', 'v12']
    }


    def "writeDataToCsvFile file data date format"() {
        given:
        def mdt
        def data
        def file
        def fdata
        Throwable e
        final SimpleDateFormat SQL_DEVELOPER_IMPORT_DATE_FORMAT = new SimpleDateFormat("dd-M-yyyy")
        def d1 = SQL_DEVELOPER_IMPORT_DATE_FORMAT.parse('31-12-1999')
        def d2 = SQL_DEVELOPER_IMPORT_DATE_FORMAT.parse('15-05-2020')
        def outDataFile = new File('build/mdt-test.csv')
        //def outMetaFile = new File('build/mdt-test.yaml')

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.writeMetaFile(new File('build'))
        mdt.dataAdd(id:'id1', v1:d1)
        mdt.dataAdd(id:'id2', v1:d2)
        file = mdt.writeDataToCsvFile(outDataFile)

        then:
        file.exists()

        when:
        fdata = DataTable.readDataFromCsvFile(file)

        then:
        fdata.size() == 2
        fdata[0]['ID'] == 'id1'
        fdata[0]['V1'] == SqlUtils.DEFAULT_TIMESTAMP_FORMATER.format(d1)
        fdata[1]['ID'] == 'id2'
        fdata[1]['V1'] == SqlUtils.DEFAULT_TIMESTAMP_FORMATER.format(d2)

        when:
        mdt.dateFormat = SQL_DEVELOPER_IMPORT_DATE_FORMAT
        mdt.writeMetaFile(new File('build'))
        mdt.dataAdd(id:'id3', v1:d1)
        mdt.dataAdd(id:'id4', v1:d2)
        file = mdt.writeDataToCsvFile(outDataFile)
        fdata = DataTable.readDataFromCsvFile(file)

        then:
        fdata.size() == 4
        fdata[2]['ID'] == 'id3'
        fdata[2]['V1'] == SQL_DEVELOPER_IMPORT_DATE_FORMAT.format(d1)
        fdata[3]['ID'] == 'id4'
        fdata[3]['V1'] == SQL_DEVELOPER_IMPORT_DATE_FORMAT.format(d2)

    }


    ///////////////////////////////////////////////////////////////////////////
    // dataAddAll
    ///////////////////////////////////////////////////////////////////////////
    
    def "dataAddAllGroovyRowResults"() {
        given:
        def mdt
        Collection<GroovyRowResult> res
        def d
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def dl = [
            [id:'id1', d1:'d11'],
            [id:'id2', d1:'d12']
        ] as Collection<GroovyRowResult> 
        res = dl.collect { it as GroovyRowResult }
        mdt.dataAddAllGroovyRowResults(res)
        d = mdt.data

        then:
        d.size() == 2
        d['id1'].size() == 2
        d['id2'].size() == 2
        d['id1']['ID'] == 'id1'
        d['id1']['D1'] == 'd11'
        d['id2']['ID'] == 'id2'
        d['id2']['D1'] == 'd12'        
    }


    @InheritConstructors
    static class TestCsvIterator extends CsvIterator {
        Map cols = [:]
        List pms = []
        TestCsvIterator() {
            super([], null)
        }
        TestCsvIterator(Map cols) {
            super([], null)
            this.cols = cols
        }
        void add(PropertyMapper pm) {
            pms << pm
        }
        void add(List vals, Map cols) {
            pms << new PropertyMapper(values: vals, columns: cols)
        }
        void add(List vals) {
            pms << new PropertyMapper(values: vals, columns: cols)
        }
        Collection each(Closure cl) {
            pms.each(cl)
        }
    }


    def "dataAddAll CsvIterator"() {
        given:
        def mdt
        def res
        def d
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = new TestCsvIterator(id:0, d1:1)
        res.with {
            add(['id1', 'd11'])
            add(['id2', 'd12'])
        }
        mdt.dataAddAll(res)
        d = mdt.data

        then:
        d.size() == 2
        d['id1'].size() == 2
        d['id2'].size() == 2
        d['id1']['ID'] == 'id1'
        d['id1']['D1'] == 'd11'
        d['id2']['ID'] == 'id2'
        d['id2']['D1'] == 'd12'
    }



    def "dataAddAllListList duplicate keys"() {
        given:
        def mdt
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'd1']
            , ['id1', 'd11']
            , ['id1', 'd12y']
        ])

        then:
        e = thrown()
    }


    def "dataAddAllListList data rows must be of right size"() {
        given:
        def mdt
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'd1']
            , ['id1']
        ])

        then:
        e = thrown()
    }


    def "dataAddAllListList idFieldName must be in dataToAdd"() {
        given:
        def mdt
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['idx', 'd1']
            , ['id1', 'd11']
        ])

        then:
        e = thrown()
    }



    def "dataAddAllListList id fields are unique case-insensitive"() {
        given:
        def mdt
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['Id', 'id']
            , ['id1', 'd11']
        ])

        then:
        e = thrown()
    }



    def "dataAddAllListList id field names"() {
        given:
        def mdt
        def data
        Throwable e

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['id', 'd1']
            , ['id1', 'd11']
        ])

        then:
        mdt.keySet.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'D1' }
        mdt.data.size() == 1

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['Id', 'd1']
            , ['id1', 'd11']
        ])

        then:
        mdt.keySet.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'D1' }
        mdt.data.size() == 1

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAddAllListList([
            ['ID', 'd1']
            , ['id1', 'd11']
        ])

        then:
        mdt.keySet.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'D1' }
        mdt.data.size() == 1

    }




    ///////////////////////////////////////////////////////////////////////////
    // dataAdd
    ///////////////////////////////////////////////////////////////////////////



    def "dataAdd ResultSet"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = [
            getMetaData: { [
                getColumnCount: { 
                    return 2 
                },
                getColumnLabel: { i -> 
                    switch (i) {
                        case 1: return 'id';
                        case 2: return 'd1';
                        default: throw new RuntimeException("$i") 
                    } 
                }
            ] as java.sql.ResultSetMetaData },
            getObject: { Object key ->
                switch (String.valueOf(key)) {
                    case 'id': return 'id1';
                    case 'd1': return 'd11';
                    default: throw new RuntimeException("$key") 
                } 
            }
        ] as java.sql.ResultSet
        mdt.dataAdd(res)

        then:
        mdt.data['id1'].size() == 2

        when:
        res = mockResultSet(id:'id1', d1:'d11')
        mdt.dataAdd(res)

        then:
        e = thrown()

        when:
        res = mockResultSet(id_:'id2', d1:'d12')
        mdt.dataAdd(res)

        then:
        e = thrown()

        when:
        res = mockResultSet(ID:'id2', d1:'d12')
        mdt.dataAdd(res)

        then:
        mdt.data['id2'].size() == 2
    }



    def "dataAdd PropertyMapper"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = new PropertyMapper(
            values: ['id1', 'd11'],
            columns: [id:0, d1:1]
        )
        mdt.dataAdd(res)

        then:
        mdt.data['id1'].size() == 2

        when:
        res = new PropertyMapper(
            values: ['id1', 'd11'],
            columns: [id:0, d1:1]
        )
        mdt.dataAdd(pm)

        then:
        e = thrown()

        when:
        res = new PropertyMapper(
            values: ['id2', 'd12'],
            columns: [id_:0, d1:1]
        )
        mdt.dataAdd(res)

        then:
        e = thrown()

        when:
        res = new PropertyMapper(
            values: ['id2', 'd12'],
            columns: [ID:0, d1:1]
        )
        mdt.dataAdd(res)

        then:
        mdt.data['id2'].size() == 2
    }


    def "dataAdd GroovyRowResult"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        res = [id:'id1', d1:'d11'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        mdt.data['id1'].size() == 2

        when:
        res = [id:'id1', d1:'d11'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        e = thrown()

        when:
        res = [id_:'id2', d1:'d12'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        e = thrown()

        when:
        res = [ID:'id2', d1:'d12'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        mdt.data['id2'].size() == 2
    }


    def "dataAdd Map date values"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def cal = Calendar.getInstance()

        // 2002-2-3 11:13AM
        cal.set(2002, Calendar.FEBRUARY, 03) 
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 13)
        cal.set(Calendar.SECOND, 27)

        def data1 = [ID:'id1', v1:cal.time]
        mdt.dataAdd(data1)

        then:
        mdt.data.size() == 1

        when:
        def d1 = mdt.dataGet('id1')

        then:
        d1['V1'] == SqlUtils.timestampAsString(cal.time)
    }


    def "dataAdd Map values multiple ids"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def data1 = [ID:'id1', v1:'v11']
        mdt.dataAdd(data1)

        then:
        mdt.data.size() == 1
        matchData(mdt, 'id1', data1)

        when:
        def data2 = [ID:'id2', v1:'v12']
        mdt.dataAdd(data2)

        then:
        mdt.data.size() == 2
        matchData(mdt, 'id1', data1)
        matchData(mdt, 'id2', data2)
    }


    def "dataAdd secondary id fields"() {
        given:
        def mdt = new MappedDataTable(name:"mdt-test", idFieldName:"id", idKeyType:KeyType.EMPI, secondaryIdFieldMap:["id2":KeyType.MRN, "id3":KeyType.PK_PATIENT_ID])
       
        when:
        mdt.dataAdd(data)

        then:
        matchData(mdt, idVal, expectedData)

        where:
        expectedData                                  | data                                          | idVal
        [ID:"id1val1", ID2:"id2val", ID3:"id3val"]    | [ID:"id1val1", ID2:"id2val", ID3:"id3val"]    | "id1val1"
        [ID:"id1val2", ID2:"id2val", ID3:"id3val"]    | [id:"id1val2", id2:"id2val", id3:"id3val"]    | "id1val2"
        [ID:"id1val3", ID2:"id2val", ID3:"id3val"]    | [ID:"ID1val3", ID2:"id2val", ID3:"id3val"]    | "id1val3"
        [ID:"id1val4", ID2:"id2val", ID3:"id3val"]    | [ID:"id1val4", ID2:"ID2val", ID3:"id3val"]    | "id1val4"
        [ID:"id1val5", ID2:"id2val", ID3:"id3val"]    | [ID:"id1val5", ID2:"id2val", ID3:"ID3val"]    | "id1val5"
        [ID:"id1val6", ID2:"id2val"]                  | [ID:"id1val6", ID2:"id2val"]                  | "id1val6"
        [ID:"id1val7", ID2:"id2val", ID3:null]        | [ID:"id1val7", ID2:"id2val", ID3:null]        | "id1val7"
    }


    def "dataAdd Map values single id"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def m = [ID:id]
        m.putAll(data)
        mdt.dataAdd(m)

        then:
        matchData(mdt, id, m)

        where:
        id            | data
        'id1'         | [V1:'v11']
        'id1'         | [V1:'v11', V2:'v21']
        'id1'         | [V1:11]
        //'id1'         | [V1:new Date()]
    }


    void matchData(MappedDataTable mdt, String id, Map data) {
        def mdtDataMap = mdt.dataGet(id)
        assert mdtDataMap
        assert mdtDataMap.size() > 0

        data.each { k, v ->
            def fn = DataTable.toFieldName(k)
            assert mdtDataMap.containsKey(fn)

            def mdtValue = mdtDataMap.get(fn)
            assert (mdtValue == v) || (mdtValue == String.valueOf(v))
        }
        assert mdtDataMap?.keySet()?.size() == data?.keySet()?.size()
    }


    def "dataAdd Map field names are cleaned up"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def m = [id:'1']
        m.put(fn, 'v1')
        mdt.dataAdd(m)

        then:
        mdt.keySet.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'A' }

        where:
        fn << ['a', ' a', 'a ', 'A']
    }


    def "dataAdd Map id values are cleaned up"() {
        given:
        def mdt
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        mdt.dataAdd(id:idVal, v1:'v11')
        def d = mdt.dataGet(idVal)

        then:
        d.size() == 2
        d['ID'] == 'a'

        where:
        idVal << ['a', ' a', 'a ', 'A']
    }


    def "dataAdd Map multiple matching id field names fail"() {
        given:
        def mdt
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def m = [:]
        m.put('id', 'id-a')
        m.put(fn, "id-${cnt}")
        mdt.dataAdd(m)

        then:
        e = thrown()

        where:
        fn << [' id', 'id ', 'ID', 'Id', 'iD']
        cnt << (1..5).collect { "$it" }
    }



    def "dataAdd Map keys are mapped to field names"() {
        given:
        def mdt
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        def m = [:]
        m.put(idKey, idVal)
        m.put('v1', 'v11' )
        mdt.dataAdd(m)

        then:
        mdt.dataGet(idVal).size() == 2

        where:
        idKey << ['id', ' id', 'id ', 'ID', 'Id', 'iD']
        idVal << (1..6).collect { "$it" }
    }


    def "dataAdd Map cannot add same id twice"() {
        given:
        def mdt
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)
        mdt.dataAdd(id:'id1', v1:'v11')

        when:
        mdt.dataAdd(id:idstr, v2:'v12')

        then:
        e = thrown()

        where:
        idstr << ['id1', ' id1', 'id1 ', 'ID1', 'Id1', 'iD1']
    }


    def "dataAdd Map basics"() {
        given:
        def mdt
        Throwable e
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id', idKeyType:KeyType.EMPI)

        when:
        mdt.dataAdd(id:'id1', v1:'v11')

        then:
        mdt.dataGet('id1').size() == 2

        // cannot add id1 twice
        when:
        mdt.dataAdd(id:'id1', v2:'v21')

        then:
        e = thrown()

        // does not contain key 'id'
        when:
        mdt.dataAdd(id_:'id1', v1:'v11')

        then:
        e = thrown()

        // multiple id key matches
        when:
        mdt.dataAdd(id:'id1', ID:'id1')

        then:
        e = thrown()

        when:
        mdt.dataAdd(ID:'id2', v1:'v12')

        then:
        mdt.dataGet('id2').size() == 2
    }


    def "MappedDataTable constructor formats idFieldName as a field name"() {
        given:
        def mdt

        when:
        mdt = new MappedDataTable(name:'mdt-test', idFieldName:fn, idKeyType:KeyType.EMPI)

        then:
        mdt.idFieldName == 'ID'

        where:
        fn << ['id', ' id', 'id ', 'ID', 'Id', 'iD']
    }

    def "setOrderedKeys input"() {
        given:
        def dt = new GenericDataTable(name:"ordered-keys-test")

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0

        when:
        dt.setOrderedKeys(keyVals)

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == 0

        when:
        dt.dataAdd(inputRow)

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == expected.size()
        assertExpectedKeys(dt, expected)

        where:
        keyVals         | expected      | inputRow
        []              | ['A']         | [a:"test"]
        []              | ['A', 'B']    | [a:"test1", b:"test2"]
        []              | ['A', 'B']    | [A:"test1", B:"test2"]
        []              | ['B', 'A']    | [b:"test1", a:"test2"]
    }

    private void assertExpectedKeys(DataTable dt, List<String> expected) {
        assert dt.keySet.size() == expected.size()
        dt.keySet.eachWithIndex { k, i ->
            //println "$k $i ${expected[i]}"
            assert k == expected[i]
        }
        //expected.each { ev -> assert dt.keySet.find { it == ev } }
    }


}