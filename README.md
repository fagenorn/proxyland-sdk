# Getting Started
Android SDK for [proxyland.io](https://proxyland.io/)
# Usage
## Gradle
Add the private repository.
```groovy
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/fagenorn/proxyland-sdk")
        credentials {
            username = "fagenorn"
            password = "ACCESS_TOKEN"
        }
    }
}
```
And include the library itself.
```groovy
dependencies {
    implementation 'com.betroix.proxyland:proxyland-sdk'
}
```
## Initialization

### Java
```java
import com.betroix.proxyland.ProxylandSdk;

public class MainActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProxylandSdk.initializeAsync(this, "PARTNER_ID");
    }
}
```
### Kotlin
```kotlin
import com.betroix.proxyland.ProxylandSdk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ProxylandSdk.initializeAsync(this, "PARTNER_ID")
    }
}
```
