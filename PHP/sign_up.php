<?php

    error_reporting(E_ALL);
    ini_set('display_errors',1);

    include('dbcon_capstone.php');

    $android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

    if( (($_SERVER['REQUEST_METHOD'] == 'POST') && isset($_POST['submit'])) || $android )
    {

      // 안드로이드 코드의 postParameters 변수에 적어준 이름을 가지고 값을 전달 받습니다.
      $email=$_POST['email'];
      $password=$_POST['password'];
      $name=$_POST['name'];
      $phone=$_POST['phone'];

      $sql="select * from members where email='$email'";  // 같은이메일로 가입 못하게
      $stmt = $con->prepare($sql);  //준비
      $stmt->execute();   //sql 실행

        if($stmt->rowCount() != 0) //
        {
          echo "(오류) 이미 회원가입된 이메일 입니다.";
        }
        else
        {
          try{
              // SQL문을 실행하여 데이터를 MySQL 서버의 person 테이블에 저장합니다.
              // fee 테이블에 있는 예약 가능 여부도 갱신합니다.
              $stmt = $con->prepare('INSERT INTO members(email, password,name,phone,havetopay) VALUES(:email, :password, :name, :phone,0);');

              $stmt->bindParam(':email', $email);
              $stmt->bindParam(':password', $password);
              $stmt->bindParam(':name', $name);
              $stmt->bindParam(':phone', $phone);
              if($stmt->execute())
              {
                  $successMSG = "회원가입 완료";
              }
              else
              {
                  $errMSG = "(오류) 잠시후 다시 시도해 주세요.";
              }

          } catch(PDOException $e) {   // 에러 (입력이 제대로 안됨)
              die("Database error: " . $e->getMessage());
          }
      } // else
} //

?>

<?php
    if (isset($errMSG)) echo $errMSG;
    if (isset($successMSG)) echo $successMSG;

	$android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

    if( !$android )
    {
?>
    <html>
       <body>

            <form action="<?php $_PHP_SELF ?>" method="POST">
                email: <input type = "text" name = "email" />
                password: <input type = "text" name = "password" />
                name: <input type = "text" name = "name" />
                phone: <input type = "text" name = "phone" />
                <input type = "submit" name = "submit" />
            </form>

       </body>
    </html>

<?php
    }
?>
