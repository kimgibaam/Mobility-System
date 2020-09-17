<?php
error_reporting(E_ALL);
ini_set('display_errors',1);

include('dbcon_capstone.php');

$android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

//POST 값을 읽어온다.
$email=isset($_POST['email']) ? $_POST['email'] : '';
$password=isset($_POST['password']) ? $_POST['password'] : '';

if ($email != ""  && $password != ""){

    $sql="select * from members where email='$email' and password = '$password'";
    $stmt = $con->prepare($sql);
    $stmt->execute();

  if ($stmt->rowCount() == 0)
  {
      echo "Error";    // 조회실패 , 데이터베이스에 없는 값
  }
	else
  {
    $data = array();

    while($row=$stmt->fetch(PDO::FETCH_ASSOC))
    {
        extract($row);

        array_push($data,
            array('havetopay'=>$havetopay
        ));
    }

    echo $havetopay;

  }
}

?>

<?php

$android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

if (!$android){
?>

<html>
   <body>

      <form action="<?php $_PHP_SELF ?>" method="POST">
          email : <input type = "text" name = "email" />
          password : <input type = "text" name = "password" />
         <input type = "submit" />
      </form>

   </body>
</html>
<?php
}

?>
