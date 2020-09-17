import numpy as np 
import pandas as pd 
import matplotlib.pyplot as plt
rawdata = pd.read_csv('./mobike_shanghai_sample_updated.csv')

#데이터 확인
rawdata.head()  

locationset=set()
#중복된 위치 제거 작업 set 활용
#데이터는 서비스 사용 시작 위치만을 활용하였음
for i in range(len(rawdata)):
    locationset.add((rawdata.start_location_y[i],rawdata.start_location_x[i]))
print("중복없는 위치의 총 개수 : ",len(locationset))

#리스트로 변환
locationset=list(locationset)

#이번엔 위치를 기반으로 넘파이배열을 만들어 중복된 위치의 개수를 구함
countfolocation=np.zeros(len(locationset))
for i in range(len(rawdata)):
    countfolocation[locationset.index((rawdata.start_location_y[i],rawdata.start_location_x[i]))]+=1  
print("각 위치의 빈도수")
countfolocation=list(countfolocation)
# 14개만 출력
print(countfolocation[:14])

#특정 범위 이하 값은 노이즈 취급하여 제거
for i in range(len(countfolocation)-1,-1,-1):
    if countfolocation[i]<5:
        del countfolocation[i]
        del locationset[i]

#!pip install folium

#위도 경도의 중복 없는 값을 맵에 뿌려준다.
x=list((locationset[i][0]) for i in range(len(locationset)))
y=list((locationset[i][1]) for i in range(len(locationset)))
import folium      #  지도 라이브러리
from   folium.plugins import MarkerCluster
from statistics import mean

#위도 경도의 평균값이 지도의 초기 위치로 설정됨
map_world = folium.Map(location=[mean(x), mean(y)], tiles = 'OpenStreetMap', zoom_start = 12)

# 위치를 전부 맵에다 마커로 찍어줌
for i in range(len(x)):
    folium.CircleMarker(
        [x[i], y[i]],
        radius=4*(countfolocation[i]/10000),
        popup=countfolocation[i],
        fill=True,
        color='Red',
        fill_color='Red',
        fill_opacity=0.6
        ).add_to(map_world)

# 맵 출력
map_world

from sklearn.cluster import KMeans
from sklearn import metrics
from scipy.spatial.distance import cdist
X = np.array(list(zip(x, y))).reshape(len(x), 2)
distortions = []

#최적의 k 값을 찾기 위한 작업
K = range(1,15)
for k in K:
    kmeanModel = KMeans(n_clusters=k).fit(X)
    kmeanModel.fit(X)
    distortions.append(sum(np.min(cdist(X, kmeanModel.cluster_centers_, 'euclidean'), axis=1)) / X.shape[0])

# k평균 알고리즘의 k의 크기에 따라 달라지는 왜곡의 정도 plotting
plt.plot(K, distortions, 'bx-')
plt.xlabel('k')
plt.ylabel('Distortion')
plt.title('The Elbow Method showing the optimal k')
plt.show()

# 학습
kmeanModel = KMeans(n_clusters=14).fit(X)

# 클러스터링 한 위치들의 중심점, 새로운 주차장
centers = kmeanModel.cluster_centers_

#최적의 장소 예측
y_kmeans = kmeanModel.predict(X)
print("처음 열네 곳에서 가장 가까운 새로운 주차장 위치 : ",y_kmeans[:14])

print("예측된 장소들의 중심점 위치들")
print(centers)

newstation=np.zeros(14)
for i in range(len(y_kmeans)):
    newstation[y_kmeans[i]]+=countfolocation[i]
print("새로운 위치에 의해 계산된 위치 수")
print(newstation)

import numpy
sortedstation=(numpy.argsort(newstation))
print("빈도수별 정렬")
print(sortedstation)

sortedcenter=[]
print("주변에 따라 정렬된 센터 무게")
for i in range(len(sortedstation)-1,-1,-1):
    sortedcenter.append(list(centers[sortedstation[i]]))
    print(sortedcenter[len(sortedcenter)-1])
#print(sortedcenter)

map_world = folium.Map(location=[mean(x), mean(y)], tiles = 'OpenStreetMap', zoom_start = 12)

# 클러스터 범위 마커
for i in range(len(centers)):
    folium.CircleMarker(
        [sortedcenter[i][0], sortedcenter[i][1]],
        radius=10*(9-i),
        popup=i+1,
        fill=True,
        color='Green',
        fill_color='Green',
        fill_opacity=0.2
        ).add_to(map_world)
    
# 위치의 중심점 마커
for i in range(len(centers)):
    folium.CircleMarker(
        [sortedcenter[i][0], sortedcenter[i][1]],
        radius=2,
        popup=i+1,
        fill=True,
        color='Blue',
        fill_color='Blue',
        fill_opacity=0.6
        ).add_to(map_world)
    
map_world