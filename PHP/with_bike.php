<?

error_reporting(E_ALL);
ini_set('display_errors',1);

include('dbcon_capstone.php');

  $id=$_GET['id'];
  $lati=$_GET['lati'];
  $longi=$_GET['longi'];

  $id2 = $id;


  $sql="UPDATE target SET lati =:lati, longi =:longi  WHERE id = :id";
  $stmt = $con->prepare($sql);
  $stmt->bindParam(':id', $id);
  $stmt->bindParam(':lati', $lati);
  $stmt->bindParam(':longi', $longi);
  $stmt->execute();

    echo "#";
    $sql="SELECT positive FROM target  WHERE id = :id";
    $stmt = $con->prepare($sql);
    $stmt->bindParam(':id', $id2);
    $stmt->execute();

    $data = array();

    while($row=$stmt->fetch(PDO::FETCH_ASSOC))
    {
        extract($row);

        array_push($data,
            array('positive'=>$positive
        ));
    }

    echo $positive;
    echo "#";

?>
