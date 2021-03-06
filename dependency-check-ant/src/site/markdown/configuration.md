Configuration
====================
To configure the dependency-check task you can add it to a target and include a
file based [resource collection](http://ant.apache.org/manual/Types/resources.html#collection)
such as a [FileSet](http://ant.apache.org/manual/Types/fileset.html), [DirSet](http://ant.apache.org/manual/Types/dirset.html),
or [FileList](http://ant.apache.org/manual/Types/filelist.html) that includes
the project's dependencies.

```xml
<target name="dependency-check" description="Dependency-Check Analysis">
    <dependency-check applicationname="Hello World"
                      reportoutputdirectory="${basedir}"
                      reportformat="ALL">

        <fileset dir="lib">
            <include name="**/*.jar"/>
        </fileset>
    </dependency-check>
</target>
```
The following table lists the configurable properties:

Property              | Description | Requirement
----------------------|-------------|---------
ApplicationName       | The name of the application to use in the generated report. | Required
ReportFormat          | The format of the report to be generated. Allowed values are: HTML, XML, VULN, or ALL. The default value is HTML.| Optional
ReportOutputDirectory | The directory where dependency-check will store data used for analysis. Defaults to the current working directory. | Optional
FailBuildOn           | If set and a CVE is found that is greater then the specified value the build will fail. The default value is 11 which means that the build will not fail. Valid values are 0-11. | Optional
AutoUpdate            | If set to false the NVD CVE data is not automatically updated. Setting this to false could result in false negatives. However, this may be required in some environments. The default value is true. | Optional
DataDirectory         | The directory where dependency-check will store data used for analysis. Defaults to a folder called, called 'dependency-check-data', that is in the same directory as the dependency-check-ant jar file was installed in. *It is not recommended to change this.* | Optional
ProxyUrl              | Defines the proxy used to connect to the Internet. | Optional
ProxyPort             | Defines the port for the proxy. | Optional
ConnectionTimeout     | The connection timeout used when downloading data files from the Internet. | Optional


